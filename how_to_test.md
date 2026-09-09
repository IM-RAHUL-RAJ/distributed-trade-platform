# How to Test — Step by Step

A complete, ordered walkthrough: start from a clean slate, run the platform,
place an order, watch it flow through the outbox → Kafka → executor pipeline,
verify it in both databases, and finish by pruning everything back.

All commands run from the repo root (`~/Projects/trade-platform`).

---

## Step 0 — Prune Docker & reclaim disk space

Do this **before** everything (clean slate). It removes unused images, build
cache and old volumes, reclaiming gigabytes back to the host VM.

```bash
docker compose down

# drop every cached/dangling image and build cache
docker image prune -af
docker builder prune -af

# remove leftover unused volumes (also wipes h2data/s2data -> fresh DBs later)
docker volume prune -f

# optional: hard reset to factory defaults if disk is still tight
docker system prune -af --volumes

# check how much you reclaimed
docker system df
```

> Everything after this step assumes a **clean slate**: databases start empty
> because the volumes were removed.

---

## Step 1 — Build the images

```bash
docker compose build
```

Builds all services. To rebuild just one:

```bash
docker compose build service-1
docker compose build service-2
```

---

## Step 2 — Start the stack & verify health

```bash
docker compose up -d --build
docker compose ps
```

Wait ~30–60 s and confirm every row reads `healthy` (or `running` where no
healthcheck exists):

| Component  | Container                  | Host port | Role |
|------------|----------------------------|-----------|------|
| Kafka      | `trade-platform-kafka-1`   | 9092      | message bus |
| Mock data  | `trade-platform-mock-funksance-1` | 8080 | quotes + trades feed |
| Service 1  | `trade-platform-service-1-1` | 8081    | orders + order **outbox**, own read-model H2 |
| Service 2  | `trade-platform-service-2-1` | 8082    | order executor, cash/positions/trades, own H2 |
| BFF        | `trade-platform-bff-1`     | 3000      | API gateway + auth |
| Frontend   | `trade-platform-frontend-1` | 4200    | Angular UI |

Each service owns **its own** H2 file database (no shared DB):

- **Service 1** read model — volume `h2data` → `/data/trade_platform.mv.db`,
  table sets: instruments, orders, accounts, market_data, latest_events,
  **order_placed_outbox**. H2 TCP **9095**.
- **Service 2** execution ledger — volume `s2data` →
  `/data/s2_trade_platform.mv.db`, tables: orders (execution), accounts/cash,
  positions, trades, transactions, market_data, **processed_events** (idempotency).
  H2 TCP **9096**.

---

## Step 3 — See what Kafka is carrying (optional but fun)

Three topics exist: `order-placed` (S1 → S2), `trade-events` (S2 → S1),
`market-data` (mock-funksance → both).

Open two extra terminals and tail the money topics live:

```bash
# terminal A: orders leaving Service 1
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic order-placed

# terminal B: execution results coming back
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic trade-events
```

Consumer groups & lag (repeat after Step 6 to see offsets advance):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-1-group
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-2-executor
```

---

## Step 4 — Register & login a user

Needs `firstName` + `lastName` (2+ chars) and a strong password:

```bash
curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!","firstName":"Test","lastName":"User"}'

curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!"}'
```

Copy the `accessToken` from the login response and set:

```bash
TOKEN=<paste-accessToken-here>
```

Confirm the seed data is in Service 1's DB (12 instruments):

```bash
tools/h2sh.sh -sql "SELECT symbol, name, last_price FROM instruments ORDER BY symbol"
```

---

## Step 5 — Place a market BUY order

```bash
curl -s -X POST http://localhost:3000/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"symbol":"AAPL","side":"BUY","quantity":10,"orderType":"MARKET"}'
```

Note the returned `id` and the status `PENDING`. Press the terminal tabs open
from Step 3 — you should see the event flash by within ~2 seconds.

---

## Step 6 — Confirm the order left Service 1 (outbox)

Check the order and the outbox row in **Service 1's** DB:

```bash
tools/h2sh.sh -sql "SELECT symbol, side, quantity, status FROM orders ORDER BY created_at DESC LIMIT 3"

# THE OUTBOX — status must flip to PUBLISHED a second or two after placing:
tools/h2sh.sh -sql "SELECT order_id, status, created_at FROM order_placed_outbox ORDER BY created_at DESC LIMIT 3"
```

Expect:

- `orders` → `PENDING`
- `order_placed_outbox` → `PUBLISHED`

> If the outbox row stays `PENDING`, the `OutboxPublisher` didn't run: check
> `docker compose logs service-1 | grep Outbox` and confirm service-1 has
> scheduling enabled.

---

## Step 7 — Confirm Service 2 executed it (execution ledger)

Same orders story, in **Service 2's** DB (the S1 DB has no trades — that's the
point of the split). Let `S2` be a helper for the service-2 shell:

```bash
S2='docker compose exec service-2 sh -c "cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && exec java -cp h2.jar org.h2.tools.Shell -url \"jdbc:h2:tcp://localhost:9096/s2_trade_platform\" -user sa -password \"\" -sql'
```

Execution order — expect `EXECUTED`:

```bash
eval "$S2 \"SELECT id, symbol, side, quantity, status, executed_price FROM orders ORDER BY created_at DESC LIMIT 3\""
```

Idempotency ledger — one `processed_events` row per order event (proves the
event was applied exactly once):

```bash
eval "$S2 \"SELECT event_type, order_id, processed_at FROM processed_events ORDER BY processed_at DESC LIMIT 3\""
```

Also confirm live prices were polled in (12 rows):

```bash
eval "$S2 \"SELECT COUNT(*) AS market_data_rows FROM market_data\""
```

---

## Step 8 — Read the execution state over Service 2's internal API

Service 1 (and you) reads cash/positions/trades/transactions directly from S2.
The endpoints take the **userId** (from the login response, or from S1's
orders table). Set it:

```bash
USER_ID=<paste-userId-here>
# or: tools/h2sh.sh -sql "SELECT user_id FROM orders LIMIT 1"
```

```bash
curl -s http://localhost:8082/api/internal/accounts/$USER_ID
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/positions
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/trades
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/transactions
```

Expect a `$1,000,000` account, a 10-share AAPL position, one BUY trade, and a
`TRADE_BUY` cash transaction (balance `998829` after ~10 × ~117).

---

## Step 9 — Confirm Service 1 applied the execution result

Service 1's `TradeEventConsumer` flips the S1 order to `EXECUTED`:

```bash
tools/h2sh.sh -sql "SELECT symbol, side, quantity, status, executed_price FROM orders ORDER BY created_at DESC LIMIT 3"
```

Then view the dashboard (cash, position, P&L) at:

```bash
open http://localhost:4200
```

The dashboard shows the S1 story but fetches cash/positions/trades from S2's
internal API — same numbers as Step 8.

---

## Step 10 — (Optional) Watch the money move in deeper detail

```bash
# cash movements in Service 2
eval "$S2 \"SELECT type, amount, balance_after, created_at FROM transactions ORDER BY created_at DESC LIMIT 5\""

# the traded fill
eval "$S2 \"SELECT symbol, side, quantity, price, executed_at FROM trades ORDER BY executed_at DESC LIMIT 5\""

# position state (avg cost)
eval "$S2 \"SELECT symbol, quantity, average_price FROM positions\""
```

---

## Step 11 — Clean up & prune Docker again

Do this **last** — reclaim all the disk space the build/run consumed:

```bash
# stop the stack
docker compose down

# drop every cached/dangling image and build cache
docker image prune -af
docker builder prune -af

# remove leftover unused volumes (also wipes h2data/s2data)
docker volume prune -f

# optional: hard reset to factory defaults if disk is still tight
docker system prune -af --volumes

# verify the machine is back to lean
docker system df
```

Next time you test, start again from **Step 0** — the whole workflow is a
clean-slate round trip.

---

## Reference

### How the happy path works under the hood (outbox pattern)

1. **Place order** → BFF forwards to **Service 1**; in the **same transaction**
   the order is saved `PENDING` **and** a row is written to `order_placed_outbox`.
2. **OutboxPublisher** (@Scheduled ~2 s) sends pending rows to `order-placed`
   and marks them `PUBLISHED` — no direct Kafka call at order time (that's the
   outbox guarantee).
3. **Service 2** consumer: skips if the id is already in `processed_events` →
   lazily materializes an execution `orders` row + `accounts` row from the
   event → prices via mock-funksance → inserts `trade`, sets
   `orders → EXECUTED`, upserts `positions`, writes a cash `transaction`,
   records the event in `processed_events`, publishes `trade-events` — one txn.
4. **Service 1** consumes `trade-events` → updates the S1 order to
   `EXECUTED`/`REJECTED`.
5. **UI dashboard** → Service 1 → Service 2 internal API for cash/positions/
   trades/transactions.

### Topic summary

| Topic          | Producer            | Consumer          | Payload |
|----------------|--------------------|-------------------|---------|
| `order-placed` | S1 (outbox)        | S2                | `{orderId, userId, symbol, side, quantity, ...}` |
| `trade-events` | S2                 | S1                | `{tradeId, orderId, symbol, side, quantity, price, status, ...}` |
| `market-data`  | mock-funksance     | S1 & S2           | `{eventId, instrumentId, symbol, price, change, changePercent, timestamp}` |

### H2 reference queries

S1 read model (port 9095, helper `tools/h2sh.sh`):

```bash
tools/h2sh.sh -sql "SHOW TABLES"
tools/h2sh.sh -sql "SELECT order_id, status FROM order_placed_outbox ORDER BY created_at DESC LIMIT 10"
tools/h2sh.sh -sql "SELECT symbol, price, change_percent, timestamp FROM market_data ORDER BY symbol"
tools/h2sh.sh -sql "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank"
```

Interactive shell:

```bash
tools/h2sh.sh -interactive
```

S2 execution DB (port 9096, via service-2 shell — use the `$S2` helper above).

The H2 TCP ports are only reachable inside the compose network. For a desktop
client (DBeaver), temporarily map the port:

```yaml
service-2:
  ports:
    - "9096:9096"
```

then `docker compose up -d service-2`.