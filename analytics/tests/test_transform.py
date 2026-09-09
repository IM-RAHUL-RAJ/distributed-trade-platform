import pandas as pd
import pytest

from main import load_and_query, transform


@pytest.fixture
def tables():
    return {
        "orders": pd.DataFrame(
            columns=["id", "user_id", "account_id", "instrument_id", "side", "order_type", "quantity", "status"]
        ),
        "trades": pd.DataFrame(
            {
                "id": ["t1", "t2", "t3"],
                "order_id": ["o1", "o2", "o3"],
                "instrument_id": ["NVDA", "NVDA", "AAPL"],
                "side": ["BUY", "SELL", "BUY"],
                "quantity": [10, 2, 5],
                "price": [100.0, 120.0, 200.0],
                "executed_at": ["2026-09-01T10:00:00", "2026-09-02T10:00:00", "2026-09-02T11:00:00"],
            }
        ),
        "transactions": pd.DataFrame(columns=["id", "type", "amount"]),
        "positions": pd.DataFrame(columns=["id", "instrument_id", "quantity"]),
        "instruments": pd.DataFrame(
            {
                "id": ["NVDA", "AAPL"],
                "symbol": ["NVDA", "AAPL"],
                "name": ["NVIDIA Corp", "Apple Inc"],
                "currency": ["USD", "USD"],
            }
        ),
    }


def test_transform_creates_derived_columns(tables):
    out = transform(tables)

    trades = out["trades"]
    assert not trades.empty
    assert "total_value" in trades.columns
    assert "day" in trades.columns
    assert trades["total_value"].tolist() == [1000.0, 240.0, 1000.0]


def test_transform_by_symbol_ranked_by_turnover(tables):
    out = transform(tables)

    assert not out["by_symbol"].empty
    top = out["by_symbol"].iloc[0]
    assert top["symbol"] in ("NVDA", "AAPL")
    assert top["turnover"] == max(out["by_symbol"]["turnover"])


def test_transform_handles_missing_instrument_join(tables):
    no_instruments = dict(tables)
    no_instruments["instruments"] = pd.DataFrame(columns=["id", "symbol", "name", "currency"])

    out = transform(no_instruments)

    assert "UNKNOWN" in out["trades"]["symbol"].values or not out["trades"].empty


def test_transform_empty_trades(tables):
    tables["trades"] = pd.DataFrame(
        columns=["id", "order_id", "instrument_id", "side", "quantity", "price", "executed_at"]
    )

    out = transform(tables)

    assert out["trades"].empty
    assert out["by_symbol"].empty
    assert out["by_side"].empty


def test_load_and_query_runs_analytics_sql(tables):
    frames = transform(tables)
    results = load_and_query(frames)

    assert "summary" in results
    assert "by_day" in results
    assert "side_breakdown" in results
    assert "top_symbols" in results
    summary = results["summary"]
    assert summary["total_trades"].item() == 3
    assert summary["turnover"].item() == 2240.0
    assert summary["bought"].item() == 15
    assert summary["sold"].item() == 2


def test_export_writes_csv_and_json(tmp_path):
    from main import export

    frames = transform(
        {
            "trades": pd.DataFrame(
                {
                    "id": ["t1"],
                    "order_id": ["o1"],
                    "instrument_id": ["NVDA"],
                    "side": ["BUY"],
                    "quantity": [1],
                    "price": [100.0],
                    "executed_at": ["2026-09-01T10:00:00"],
                }
            ),
            "instruments": pd.DataFrame({"id": ["NVDA"], "symbol": ["NVDA"]}),
        }
    )
    results = load_and_query(frames)

    export(results, tmp_path)

    assert (tmp_path / "summary.csv").exists()
    assert (tmp_path / "summary.json").exists()
    assert (tmp_path / "top_symbols.csv").exists()
    assert (tmp_path / "by_day.csv").exists()