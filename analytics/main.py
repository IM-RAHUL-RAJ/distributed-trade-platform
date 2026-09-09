"""Analytics: Pandas + DuckDB batch job.

Extract relevant trading data from PostgreSQL, transform it with Pandas,
load it into DuckDB and run analytical queries. Outputs CSV/JSON reports
to ./output. Designed to later run as a Kubernetes CronJob.
"""
from __future__ import annotations

import json
import os
from pathlib import Path

import pandas as pd


def extract(conn):
    """Pull trading tables into DataFrames."""
    tables = {}
    for name in ("orders", "trades", "transactions", "positions", "instruments"):
        tables[name] = pd.read_sql_query(f"SELECT * FROM {name}", conn)
    return tables


def transform(tables: dict) -> dict:
    """Dimension + measure transformations via Pandas."""
    trades = tables["trades"].copy()
    instruments = tables["instruments"].copy()

    if not trades.empty:
        trades["total_value"] = trades["quantity"] * trades["price"]
        trades["day"] = pd.to_datetime(trades["executed_at"]).dt.date

        merged = trades.merge(
            instruments[["id", "symbol"]],
            left_on="instrument_id",
            right_on="id",
            how="left",
            suffixes=("", "_inst"),
        )
        if "symbol" not in merged.columns or merged["symbol"].isna().any():
            merged["symbol"] = merged.get("symbol", "").fillna("UNKNOWN").astype(str)

        direction = merged["side"].map({"BUY": 1.0, "SELL": -1.0})
        merged["realized_pnl"] = direction * (merged["price"] - merged["price"].mean()) * merged["quantity"]
    else:
        merged = trades.copy()

    by_symbol = pd.DataFrame()
    if not merged.empty and "symbol" in merged.columns:
        by_symbol = (
            merged.groupby("symbol")
            .agg(
                trades=("order_id", "count"),
                quantity=("quantity", "sum"),
                turnover=("total_value", "sum"),
                realized_pnl=("realized_pnl", "sum"),
            )
            .reset_index()
            .sort_values("turnover", ascending=False)
        )

    by_side = pd.DataFrame()
    if not merged.empty:
        by_side = (
            merged.groupby("side")
            .agg(trades=("order_id", "count"), quantity=("quantity", "sum"))
            .reset_index()
        )

    return {"trades": merged, "by_symbol": by_symbol, "by_side": by_side}


def load_and_query(frames: dict, db_path: str | None = None) -> dict:
    """Load transformed frames into DuckDB and run analytical queries."""
    import duckdb

    con = duckdb.connect(db_path or ":memory:")
    for name, frame in frames.items():
        con.register(name, frame)

    summary = pd.DataFrame(con.execute(
        "SELECT COUNT(*) AS total_trades, "
        "       COALESCE(SUM(CASE WHEN side='BUY' THEN quantity ELSE 0 END),0) AS bought, "
        "       COALESCE(SUM(CASE WHEN side='SELL' THEN quantity ELSE 0 END),0) AS sold, "
        "       COALESCE(SUM(total_value),0) AS turnover, "
        "       COALESCE(SUM(realized_pnl),0) AS realized_pnl "
        "FROM trades"
    ).fetchdf())

    top_symbols = pd.DataFrame(con.execute(
        "SELECT symbol, trades, quantity, turnover, realized_pnl "
        "FROM by_symbol ORDER BY turnover DESC LIMIT 10"
    ).fetchdf())

    by_day = pd.DataFrame(con.execute(
        "SELECT day, COUNT(*) AS trades, SUM(total_value) AS turnover "
        "FROM trades GROUP BY day ORDER BY day"
    ).fetchdf())

    side_breakdown = pd.DataFrame(con.execute(
        "SELECT side, trades, quantity FROM by_side"
    ).fetchdf())

    con.close()
    return {"summary": summary, "top_symbols": top_symbols, "by_day": by_day, "side_breakdown": side_breakdown}


def export(results: dict, out_dir: Path) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, frame in results.items():
        if name == "summary":
            frame.to_json(out_dir / f"{name}.json", orient="records", indent=2)
        frame.to_csv(out_dir / f"{name}.csv", index=False)
        print(f"  wrote {name}.csv / .json ({len(frame)} rows)")


def main() -> None:
    import psycopg2

    conn = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "trade_platform"),
        user=os.getenv("DB_USER", "trade_app"),
        password=os.getenv("DB_PASSWORD", "trade_app_password"),
    )
    out_dir = Path(os.getenv("ANALYTICS_OUTPUT_DIR", "output"))

    print("[analytics] extracting from PostgreSQL ...")
    tables = extract(conn)
    conn.close()

    print("[analytics] transforming with pandas ...")
    frames = transform(tables)

    print("[analytics] loading into DuckDB and querying ...")
    try:
        results = load_and_query(frames, db_path=str(out_dir / "analytics.duckdb"))
    except Exception as exc:  # DuckDB may lack required columns on empty data
        print("[analytics] query step failed:", exc)
        results = {}

    print("[analytics] exporting reports ...")
    export(results, out_dir)
    print("[analytics] done.")


if __name__ == "__main__":
    main()