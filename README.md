# Trade Platform

A full-stack microservices trading platform running entirely with Docker Compose.
Angular UI -> NestJS BFF -> Spring Boot (Service 1: orders/instruments/accounts) -> Kafka ->
Spring Boot (Service 2: order execution + market data) <- Mock Funksance feed -> PostgreSQL.

## Architecture

```
                      +--------------------+      +--------------------------+
  Browser (4200) ---> | Nginx + Angular UI |      | Mock Funksance (price #1)|
                      +--------------------+      +------------+-------------+
                               |                                          |
                               v                                          v
                      +--------------------+   order-placed          +---------------------+
                      | NestJS BFF (3000)  | ==========Kafka========> | Service 2  (8082)    |
                      | auth + forwarding  |   <========Kafka======== | executes orders,      |
                      +---------+----------+   trade-events/market-   | publishes trade +    |
                                |                data                   | market data          |
                                v                                    +----------+----------+
                      +--------------------+    shared JWT                     |
                      | Service 1  (8081)  |<--------------- Postgres ---------+
                      | instruments/orders |   (accounts, orders, positions,
                      | preferences/...    |    transactions, trades, watchlist)
                      +--------------------+
```

- **Kafka payloads are plain JSON strings** (StringSerializer). Topics: `order-placed`,
  `trade-events`, `market-data`.
- **Service 1** validates a shared HS256 JWT and is the only service exposing REST APIs.
- **Service 2** consumes `order-placed`, executes via Mock Funksance, publishes
  `trade-events` and `market-data`; exactly-once per order via `processed_events`.
- **BFF** owns auth (hedged + refresh tokens), forwards `/api/v1/*` to Service 1 with the
  same Bearer token.
- Service 1 & 2 listen on `8080` inside their containers and are mapped to `8081` / `8082`.

## Prerequisites

- Docker Desktop 4.x (with `docker compose` v2), ~3 GB free for images + ~1 GB for data.
- Verify: `docker info` then `docker compose version`.

## Run everything

```bash
cp .env.example .env        # optional; defaults are fine for local dev
docker compose up -d --build
docker compose ps           # all 7 services should be Healthy
```

| Service         | URL                     |
|-----------------|-------------------------|
| Frontend (UI)   | http://localhost:4200   |
| BFF API         | http://localhost:3000/api/v1 |
| Service 1       | http://localhost:8081/api/v1  |
| Service 2       | http://localhost:8082          |
| Postgres        | localhost:5432 (`trade_app` / `trade_app_password`) |
| Kafka           | localhost:9092                 |

To stop: `docker compose down` (keeps Postgres/Kafka data).
To reset everything: `docker compose down -v` (deletes all data).

## Test it yourself (command-line walkthrough)

All commands assume the BFF is up. Save a token once:

```bash
EMAIL="me$(date +%s)@trading.test"
TOKEN=$(curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"firstName\":\"Test\",\"lastName\":\"User\",\"password\":\"password123\"}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
echo "token: ${TOKEN:0:20}..."
```

1. **Register + login tokens**
   ```bash
   curl -s -X POST http://localhost:3000/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}"
   ```
   -> Returns `accessToken` + `refreshToken`. Registering twice with the same email fails with 409.

2. **Instruments with live prices**
   ```bash
   curl -s http://localhost:3000/api/v1/instruments -H "Authorization: Bearer $TOKEN"
   ```
   -> Raw JSON array with AAPL/MSFT/etc. and a `lastPrice` that ticks every ~5 s
   (market data flows: Mock Funksance -> Service 2 -> Kafka -> Service 1).

3. **Account is created lazily**
   ```bash
   curl -s http://localhost:3000/api/v1/account -H "Authorization: Bearer $TOKEN"
   ```
   -> `cash: 1000000.00` the first time (margin 0). Add your own cash or update preferences
   (`PUT /api/v1/preferences`).

4. **Place a BUY market order**
   ```bash
   curl -s -X POST http://localhost:3000/api/v1/orders \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"BUY","orderType":"MARKET","quantity":100}'
   ```
   -> Returns the order as `PENDING` immediately.

5. **Order executes within seconds**
   ```bash
   sleep 10
   curl -s http://localhost:3000/api/v1/orders -H "Authorization: Bearer $TOKEN"
   ```
   -> Status becomes `EXECUTED` with an `executedPrice` (~$230). This is Service 2
   consuming from Kafka; if it is still `PENDING` after 30 s first check `docker compose logs -f service-2`.

6. **Cash decreased, position + transaction recorded**
   ```bash
   curl -s http://localhost:3000/api/v1/account -H "Authorization: Bearer $TOKEN"
   curl -s http://localhost:3000/api/v1/positions -H "Authorization: Bearer $TOKEN"
   curl -s http://localhost:3000/api/v1/transactions -H "Authorization: Bearer $TOKEN"
   ```
   -> Cash ~= 1,000,000 - (100 * price); `AAPL` position with quantity 100 and an average
   price; a `TRADE_BUY` transaction with updated balance.

7. **Trade round-trips back through Kafka into Service 1**
   ```bash
   curl -s http://localhost:3000/api/v1/trades -H "Authorization: Bearer $TOKEN"
   ```
   -> One trade row: `AAPL BUY 100 @ <price>`, even though Service 2 did the execution.

8. **Sell reduces the position**
   ```bash
   curl -s -X POST http://localhost:3000/api/v1/orders \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"SELL","orderType":"MARKET","quantity":25}'
   sleep 10
   curl -s http://localhost:3000/api/v1/positions -H "Authorization: Bearer $TOKEN"
   ```
   -> AAPL position is now 75.

9. **Buying more cash than you have is rejected**
   ```bash
   curl -s -X POST http://localhost:3000/api/v1/orders \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"BUY","orderType":"MARKET","quantity":999999}'
   ```
   -> ORDER `REJECTED` with reason `INSUFFICIENT_CASH`.

10. **Dashboard shows everything**
    Open http://localhost:4200, log in with your test email, and confirm holdings,
    open orders, recent trades and live prices render.

Backend state can be inspected directly:
```bash
docker compose exec postgres psql -U trade_app -d trade_platform -c 'select symbol,quantity,avg_price from positions;'
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic trade-events --from-beginning --max-messages 5
docker compose logs -f service-2    # watch order execution + market data produce
```

## Tests

```bash
# Service 1 & 2 (JUnit + Mockito) -- need JDK 21 + Maven
cd service-1 && mvn test && cd ../service-2 && mvn test

# BFF (Jest) -- needs Node 20
cd bff && npm ci && npm test

# Analytics (pytest + pandas + duckdb) -- venv provided
cd analytics && .venv/bin/python -m pytest -q

# Frontend (Jasmine + Karma, headless Chrome) -- needs Node 20
cd frontend && npm ci && npm test
```

Offline machine with JDK 25? Set in the poms via
`-Dmockito.version=5.18.0 -Dbyte-buddy.version=1.17.1`.

## Analytics (batch report, runs as a one-shot container)

```bash
docker compose run --rm --build analytics        # writes to /app/output (analytics-output volume)
docker compose run --rm analytics                # reuse existing image (no rebuild)
```

Run it locally instead (no Docker image needed):
```bash
cd analytics && .venv/bin/pip install -r requirements.txt && DB_HOST=localhost .venv/bin/python main.py
```
Outputs CSV/parquet summaries to `analytics/output/`.

## Saving disk space (cleanup commands)

The whole stack needs only **~3 GB images + ~1 GB data**. Everything else is reclaimable.

```bash
# 1. Remove images you no longer run (the safe ones to delete and re-pull):
docker rmi redislocal redis:7-alpine alpine:latest apache/kafka:3.7.0 2>/dev/null   # swap in your own tags

# 2. Stop a project when unused (recommended before a long break):
docker compose -f docker-compose.yml down          # containers down, images + data kept

# 3. Delete images for this project (re-download/re-build when you need them):
docker compose down
docker rmi trade-platform-service-1 trade-platform-service-2 trade-platform-bff \
          trade-platform-frontend trade-platform-mock-funksance

# 4. Nuke build cache (can be 1-2 GB after heavy builds):
docker builder prune -f

# 5. Remove EVERYTHING not currently in use (containers, anonymous volumes, unused images, cache):
docker system prune -a -f --volumes
```
> `docker system prune -a --volumes` is destructive: it removes images and volumes not
> attached to a RUNNING container. Run it only when you are OK re-creating state (it also
> wipes other projects' data volumes).

See live usage:
```bash
docker system df          # images / containers / volumes / build cache sizes
docker system df -v       # per-image breakdown
```

## Cloud: run the same stack in GitHub Codespaces

This repo includes a `.devcontainer` that starts Docker-in-Docker and loads the
**pre-built images** from a GitHub Release (no compilation happens in the cloud):

1. Open this *public* repo in Codespaces (free tier + public repo).
2. When asked for the machine type, choose the **ARM64 4-core / 8 GB** option
   (the pre-built images are `linux/arm64` — standard Intel machines will not run them).
3. The devcontainer automatically downloads `trade-platform-images.tar.gz` from
   `releases/latest`, runs `docker load`, then `docker compose up -d`.
4. Open the forwarded **4200** port for the UI; the BFF forwards as well.

Re-running after a restart: the images are cached in the Codespace, so
`docker compose up -d` alone is enough.

EC2: `docker save` the images -> `scp`/`gh release download` the tarball -> `docker load`
and `docker compose up -d`. Match CPU architecture: the tarball is `linux/arm64`, so use
Graviton (t4g/c7g) instances.

## Troubleshooting

- **Docker engine keeps stopping / "no space"** — check real disk: `df -h /`. Docker Desktop
  needs several GB of headroom on the Mac; when the host fills up the engine shuts down and
  (on iCloud-synced folders) files get evicted. Prune (see above) and keep 10+ GB free.
- **`order remains PENDING`** — Kafka metadata could be lagging after a restart:
  `docker compose restart service-2`, then check `docker compose logs -f service-2` and
  `kafka-console-consumer.sh --topic order-placed`.
- **`NOT_LEADER_OR_FOLLOWER` / `Client requested disconnect`** — transient right after the
  Kafka broker restarts; restart the consumers once the broker is healthy.
- **SQL/JWT break after a fresh DB or `.env` change** — run `docker compose down -v` to reset,
  since the schema is created by Flyway inside service containers on first boot.
- **iCloud "dataless" files** — if you work in `~/Documents`, files can be evicted to the
  cloud and read as empty. `brctl download <folder>` re-materializes them; keep disk free so
  iCloud doesn't evict again.