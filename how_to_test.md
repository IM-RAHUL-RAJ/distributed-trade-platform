# How to Test — Behind the Scenes

> **Step 0 (do this first):** reclaim Docker disk space and start from a clean slate — see the **Prune** section at the very bottom. The whole workflow below starts **and ends** with pruning.

Quick commands to inspect how the platform behaves at runtime: what's flowing
through Kafka, and what's stored in the two embedded H2 databases.

All commands are run from the repo root (`~/Projects/trade-platform`).

The stack (all local, no Postgres, no cloud):

| Component  | Container                  | Role                                     | Host port |
|------------|----------------------------|------------------------------------------|-----------|
| Kafka      | `trade-platform-kafka-1`   | message bus                              | 9092      |
| Mock data  | `trade-platform-mock-funksance-1` | markets (quotes + trades)          | 8080      |
| Service 1  | `trade-platform-service-1-1` | orders + order **outbox**, read-model H2 | 8081      |
| Service 2  | `trade-platform-service-2-1` | order executor, positions, trades, own H2 | 8082      |
| BFF        | `trade-platform-bff-1`     | NestJS API gateway + auth (sql.js)       | 3000      |
| Frontend   | `trade-platform-frontend-1` | Angular UI                              | 4200      |

Each service now owns **its own** H2 file database:

- **Service 1** read model (`h2data` volume → `/data/trade_platform.mv.db`):
  instruments, orders, accounts, market_data, latest_events, and the
  `order_placed_outbox` outbox table. H2 TCP on **9095**.
- **Service 2** execution DB (`s2data` volume → `/data/s2_trade_platform.mv.db`):
  orders (execution), accounts/cash, positions, trades, transactions, market_data
  and the `processed_events` idempotency ledger. H2 TCP on **9096**.

Service 1 tells the S1-side story; Service 2 tells the money story. Execution
results (cash, positions, trades, transactions) are read by Service 1 over
Service 2's internal HTTP API, **not** from a shared database.

---

## 1. Kafka — see what's flowing

There are 3 topics: `order-placed`, `trade-events`, `market-data`.

### List topics

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

### Describe a topic (partitions, offsets)

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic order-placed
```

### Watch a topic live (tail, like `tail -f`)

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic trade-events
```

Ctrl+C to stop. Use three terminals (order-placed / trade-events / market-data)
to watch an order travel through the pipeline while you place it in the UI.

### Replay a topic from the beginning (limit N messages)

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic order-placed --from-beginning --max-messages 5
```

### Consumer groups & lag

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --list
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-1-group
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-2-executor
```

### What each topic carries

| Topic          | Producer  | Consumer  | Payload (JSON)                                     |
|----------------|-----------|-----------|----------------------------------------------------|
| `order-placed` | Service 1 (outbox) | Service 2 | `{orderId, userId, symbol, side, quantity, ...}` |
| `trade-events` | Service 2 | Service 1 | `{tradeId, orderId, symbol, side, quantity, price, status, ...}` |
| `market-data`  | mock-funksance | Service 1 & 2 | `{eventId, instrumentId, symbol, price, change, changePercent, timestamp}` |

### Behavior behind the scenes (happy path, outbox pattern)

1. You place a BUY order → BFF forwards to **Service 1**: in the **same
   transaction** the order is saved `PENDING` **and** a row is written to
   `order_placed_outbox`.
2. **Service 1's `OutboxPublisher`** (@Scheduled, every ~2 s) publishes pending
   outbox rows to `order-placed` and marks them `PUBLISHED`. (No direct Kafka
   call at order time — that's the outbox guarantee.)
3. **Service 2** consumer picks the event up → skips it if the id is already in
   `processed_events` (idempotency) → lazily materializes an execution `orders`
   row + `accounts` row from the event → prices it via mock-funksance → inserts
   `trade`, sets `orders → EXECUTED`, upserts `positions`, writes a cash
   `transaction`, records the event id in `processed_events`, then publishes
   `trade-events` — all in one transaction.
4. **Service 1** consumer reads `trade-events` → updates the S1 order to
   `EXECUTED`/`REJECTED`.
5. The UI dashboard calls Service 1, which fetches cash/positions/trades/
   transactions from **Service 2's internal HTTP API** (`/api/internal/...`).

Watch it happen: run the topic consumers above, then place an order from
`http://localhost:4200`.

---

## 2. H2 — query the two embedded databases

Both services expose their H2 file DB over TCP inside the compose network
(user `sa`, empty password). **Service 1 on port 9095, Service 2 on port 9096.**

### Service 1 read-model DB (port 9095) — the easy helper

```bash
tools/h2sh.sh -sql "SELECT COUNT(*) AS instruments FROM instruments"
tools/h2sh.sh -interactive     # interactive shell; type SQL, exit with 'quit'
```

The helper downloads the H2 client jar into the service-1 container and runs
Shell. Raw one-liner:

```bash
docker compose exec service-1 sh -c 'cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && java -cp h2.jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9095/trade_platform" -user sa -password "" -sql "SELECT 1"'
```

### Service 2 execution DB (port 9096)

Same idea, inside the **service-2** container:

```bash
docker compose exec service-2 sh -c 'cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && java -cp h2.jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9096/s2_trade_platform" -user sa -password "" -sql "SELECT COUNT(*) FROM orders"'
```

### Useful queries — Service 1 (S1 read model)

```bash
# tables that exist
tools/h2sh.sh -sql "SHOW TABLES"

# instruments seeded by V2 migration
tools/h2sh.sh -sql "SELECT symbol, name, last_price FROM instruments ORDER BY symbol"

# recent orders + their status (S1 read model)
tools/h2sh.sh -sql "SELECT symbol, side, quantity, status, executed_price, created_at FROM orders ORDER BY created_at DESC LIMIT 10"

# THE OUTBOX — orders placed but not yet sent to Kafka
tools/h2sh.sh -sql "SELECT id, order_id, status, created_at FROM order_placed_outbox ORDER BY created_at DESC LIMIT 10"
# expect status 'PUBLISHED' for every order a few seconds after placement.
# Anything stuck in 'PENDING' = the event was never sent -> check service-1 logs
# (OutboxPublisher) or that service-1 has scheduling enabled.

# live market prices (upserted continuously)
tools/h2sh.sh -sql "SELECT symbol, price, change_percent, timestamp FROM market_data ORDER BY symbol"

# Flyway migration history
tools/h2sh.sh -sql "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank"
```

### Useful queries — Service 2 (execution ledger)

```bash
S2='docker compose exec service-2 sh -c "cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && exec java -cp h2.jar org.h2.tools.Shell -url \"jdbc:h2:tcp://localhost:9096/s2_trade_platform\" -user sa -password \"\" -sql' 

# execution orders
eval "$S2 \"SELECT id, symbol, side, quantity, status, executed_price FROM orders ORDER BY created_at DESC LIMIT 10\""

# positions (avg cost, live P&L inputs)
eval "$S2 \"SELECT symbol, quantity, average_price, updated_at FROM positions\""

# trades executed
eval "$S2 \"SELECT symbol, side, quantity, price, executed_at FROM trades ORDER BY executed_at DESC LIMIT 10\""

# cash movements
eval "$S2 \"SELECT type, amount, balance_after, created_at FROM transactions ORDER BY created_at DESC LIMIT 10\""

# idempotency ledger (each order event recorded exactly once)
eval "$S2 \"SELECT event_id, event_type, order_id, processed_at FROM processed_events ORDER BY processed_at DESC LIMIT 10\""
```

### Note

The H2 TCP ports are only reachable inside the compose network. To point a
desktop client (e.g. DBeaver) at either DB, add temporary port mapping to the
service then `docker compose up -d <service>`:

```yaml
service-1:
  # ...
  ports:
    - "9095:9095"
service-2:
  # ...
  ports:
    - "9096:9096"
```

---

## 3. Service 2's internal read API

Service 1 (and you) can read execution state directly over HTTP. The endpoints
take the **userId**. S2 returns a default `ACTIVE / $1,000,000 / 0` account if
the user has no account yet, and empty lists when nothing exists.

```bash
# cash + status (S2's real ledger)
curl -s http://localhost:8082/api/internal/accounts/<USER_ID>

# positions / trades / transactions
curl -s http://localhost:8082/api/internal/accounts/<USER_ID>/positions
curl -s http://localhost:8082/api/internal/accounts/<USER_ID>/trades
curl -s http://localhost:8082/api/internal/accounts/<USER_ID>/transactions
```

Find your USER_ID from the BFF login response, or from the S1 orders table:
`tools/h2sh.sh -sql "SELECT user_id, symbol, status FROM orders LIMIT 1"`.

---

## 4. Quick end-to-end test

```bash
# stack status
docker compose ps

# register a user (needs firstName + lastName 2+ chars)
curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!","firstName":"Test","lastName":"User"}'

# login and capture the token
curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!"}'
# -> copy accessToken

# place a market BUY
curl -s -X POST http://localhost:3000/api/v1/orders \
  -H "Authorization: Bearer <TOKEN>" -H 'Content-Type: application/json' \
  -d '{"symbol":"AAPL","side":"BUY","quantity":10,"orderType":"MARKET"}'

# within ~2 seconds the outbox row should flip to PUBLISHED and S2 should execute:
tools/h2sh.sh -sql "SELECT order_id, status FROM order_placed_outbox ORDER BY created_at DESC LIMIT 3"
tools/h2sh.sh -sql "SELECT symbol, side, quantity, status FROM orders ORDER BY created_at DESC LIMIT 3"
# and the money moved in S2 (same rows as the dashboard via http://localhost:4200):
eval "$S2 \"SELECT symbol, quantity, average_price FROM positions\""
eval "$S2 \"SELECT type, amount, balance_after FROM transactions ORDER BY created_at DESC LIMIT 3\""
```

Watch it flow live: run `kafka-console-consumer` on `order-placed` and
`trade-events` (see §1), then place the order.

---

## 5. Prune — reclaim Docker disk space

> **Do this FIRST** before any testing (clean slate), and **LAST** when you're
> done. It removes unused images + brutally reclaims space back to the host VM.

```bash
# 1) stop the stack
docker compose down

# 2) drop every cached/dangling image and build cache
docker image prune -af
docker builder prune -af

# 3) remove any leftover unused volumes (kills all data — also wipes h2data/s2data)
docker volume prune -f

# 4) (optional) hard reset to factory defaults if disk is still tight
docker system prune -af --volumes

# 5) check reclaimed space
docker system df
```

Rebuild fresh whenever you pruned:

```bash
docker compose build
docker compose up -d --build
docker compose ps            # all containers healthy
```

When you finish testing, prune again with steps 1–5 so the machine doesn't
accumulate gigabytes of image layers and build cache.