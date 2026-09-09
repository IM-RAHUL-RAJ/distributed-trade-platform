#!/usr/bin/env python3
"""
Mock Funksance market-data API.

Local stand-in for the real AWS Lambda Funksance service. Serves a JSON list
of live quotes with small random price movements so the market-data pipeline
(Service 2 -> Kafka -> Service 1 -> dashboard) can be exercised locally.

ENDPOINTS
  GET /prices        -> [{"symbol","price","change","changePercent"}, ...]
  GET /health        -> {"status":"ok"}
"""
import json
import random
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

BASE_PRICES = {
    "NVDA": 182.50, "AAPL": 232.85, "MSFT": 428.30, "TSLA": 252.40,
    "AMZN": 198.05, "META": 561.72, "GOOG": 176.15, "NFLX": 705.60,
    "AMD": 167.95, "CRM": 297.38, "DIS": 96.42, "XOM": 115.60,
}

_cached = None


def _quotes():
    global _cached
    now = time.time()
    if _cached is None:
        _cached = []
        for symbol, base in BASE_PRICES.items():
            dr = random.uniform(-0.006, 0.006)
            price = round(base * (1 + dr), 2)
            change = round(price - base, 2)
            change_percent = round((change / base) * 100, 2)
            _cached.append({
                "symbol": symbol,
                "price": price,
                "change": change,
                "changePercent": change_percent,
            })
        _cached.append(now)
        return _cached[:-1]

    stamp = _cached[-1]
    quotes = _cached[:-1]
    if now - stamp >= 2:
        _cached = None
        return _quotes()
    # jitter within cached window
    jittered = []
    for q in quotes:
        jittered.append({
            "symbol": q["symbol"],
            "price": q["price"],
            "change": q["change"],
            "changePercent": q["changePercent"],
        })
    return jittered


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def _send(self, code, obj):
        body = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.split("?")[0] == "/prices":
            self._send(200, _quotes())
        elif self.path.split("?")[0] == "/health":
            self._send(200, {"status": "ok"})
        else:
            self._send(404, {"error": "not found"})


if __name__ == "__main__":
    port = int(__import__("os").environ.get("MOCK_PORT", "8080"))
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()