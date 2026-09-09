# Trade Platform — Operations Runbook

Complete lifecycle: **start → inspect (exec / Kafka / DB) → tear down & prune → bring back up.**

All commands assume the project lives at `~/Projects/trade-platform-repo`
(`COMPOSE=~/Projects/trade-platform-repo/docker-compose.yml`).

---

## 1. Start the stack

```bash
~/Projects/trade-platform-repo/tools/tp-up.sh      # recommended (loads images if needed)
# or manually:
# docker compose -f ~/Projects/trade-platform-repo/docker-compose.yml up -d
```

Wait ~45–60 s, then confirm all services are healthy:

```bash
docker compose -f ~/Projects/trade-platform-repo/docker-compose.yml ps
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected (all `Up ... (healthy)` except the UI which shows no healthcheck):

- `trade-platform-repo-frontend-1`  (port 4200)
- `trade-platform-repo-bff-1`       (port 3000)
- `trade-platform-repo-service-1-1` (port 8081)
- `trade-platform-repo-service-2-1` (port 8082)
- `trade-platform-repo-postgres-1`  (port 5432)
- `trade-platform-repo-kafka-1`     (port 9092)
- `trade-platform-repo-mock-funksance-1`
- `trade-platform-repo-kafka-init-1` (exits 0 after creating topics — that is normal)

Open the UI: <http://localhost:4200> → Register → Login → Trade.

---

## 2. Getting container IDs / shells (`docker exec -it`)

Get the ID of any container by name:

```bash
docker ps -aqf "name=trade-platform-repo-postgres-1"      # prints the container ID
```

Exec into a shell / run a command inside a container:

```bash
# PostgreSQL — interactive SQL shell
docker exec -it trade-platform-repo-postgres-1 psql -U trade_app -d trade_platform

# Kafka — run any kafka tooling (topics, console consumer, console producer)
docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-topics.sh --list

# Service 1 & 2 — they run Bash; watch logs or poke around
docker exec -it trade-platform-repo-service-1-1 sh
docker exec -it trade-platform-repo-service-2-1 sh

# Generic: just get a shell in a container
docker exec -it <container-id> sh
```

> `-it` = interactive + TTY. Ctrl+D exits; `Ctrl+P Ctrl+Q` detaches.

---

## 3. Watch Kafka IN ACTION

### 3a. Topics & partitions

```bash
docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe order-placed
```
Expected: `order-placed` (3 partitions), `trade-events`, `market-data` (1 each).

### 3b. Consumers: watch messages stream past

Open THREE terminals. In each, start a live consumer (`--from-beginning` replays history):

| Terminal | Command |
|---|---|
| A — market data | `docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic market-data --from-beginning` |
| B — executed trades | `docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic trade-events --from-beginning` |
| C — new orders | `docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-placed --from-beginning` |

Now **place an order** in the UI (or with curl):
```bash
curl -s -X POST http://localhost:3000/api/v1/orders -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"symbol":"AAPL","side":"BUY","orderType":"MARKET","quantity":10}'
```

Observe:
- Terminal **C**: the raw order JSON appears (Service 1 → Kafka).
- Terminal **A**: prices keep ticking every ~5 s (Funksance → Service 2 → Kafka).
- Terminal **B**: the executed trade appears (Service 2 → Kafka).

### 3c. Producer: inject a message manually (Kafka without the UI)

```bash
docker exec -it trade-platform-repo-kafka-1 /bin/sh -c \
 'echo "{\"orderId\":\"MANUAL-$(date +%s)\",\"symbol\":\"TSLA\",\"side\":\"BUY\",\"orderType\":\"MARKET\",\"quantity\":1,\"accountId\":\"manual\",\"unitPrice\":null}" | \
 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-placed'
```
Then watch Service 2 pick it up:
```bash
docker compose -f ~/Projects/trade-platform-repo/docker-compose.yml logs -f service-2
```

### 3d. Consumer health (offsets / lag)

```bash
docker exec -it trade-platform-repo-kafka-1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --all-groups --describe
```
Healthy = `LAG 0`. A growing lag means consumption is stuck.

---

## 4. Check the database (tables & data)

Enter Postgres:
```bash
docker exec -it trade-platform-repo-postgres-1 psql -U trade_app -d trade_platform
```

**List all tables:**
```sql
\dt
```
Tables created by Flyway: `users`, `refresh_tokens`, `accounts`, `customer_preferences`,
`instruments`, `orders`, `trades`, `positions`, `transactions`, `watchlists`,
`market_data`, `processed_events`, `latest_events`.

**Inspect a table's shape:**
```sql
\d orders
```

**See the data after you trade** (all of these should have rows once you place orders):

```sql
-- every placed order and its lifecycle
SELECT symbol, side, quantity, status, executed_price FROM orders ORDER BY id DESC LIMIT 10;

-- current holdings
SELECT symbol, quantity, average_price FROM positions;

-- cash / margin after the trade moved money
SELECT * FROM accounts;

-- every money movement (BUY debits, SELL credits)
SELECT type, amount, balance_after FROM transactions ORDER BY id DESC LIMIT 10;

-- executed trades (what Service 2 sent back via Kafka)
SELECT symbol, side, quantity, price FROM trades ORDER BY id DESC LIMIT 10;

-- market data ticks saved by Service 1
SELECT symbol, price, timestamp FROM market_data ORDER BY timestamp DESC LIMIT 10;

-- idempotency guard: every order Service 2 processed exactly once
SELECT order_id, processed_at FROM processed_events ORDER BY processed_at DESC LIMIT 10;
```

Missing rows? That is the diagnosis:
- `orders` empty → the UI/BFF never reached Service 1.
- `orders` PENDING but `positions`/`transactions` empty → Service 2 isn't consuming
  (`docker compose logs -f service-2`, `3d` lag check).
- `trades` empty but position/transaction exist → Service 1 isn't consuming `trade-events`.

Exit with `\q`.

---

## 5. Tear everything down + prune clear EVERYTHING

```bash
# Stop the stack (keeps images + Postgres/Kafka data)
docker compose -f ~/Projects/trade-platform-repo/docker-compose.yml down

# Wipe all images, containers, caches AND volumes (Postgres/Kafka data gone)
docker system prune -a -f --volumes
```

Verify nothing is left:
```bash
docker images                  # empty
docker ps -a                   # empty
docker volume ls               # empty
docker system df               # everything ~0B
```

The VM will now be essentially empty, so it cannot bloat while unused.

---

## 6. Bring it all back up (from a fully wiped Docker)

```bash
~/Projects/trade-platform-repo/tools/tp-up.sh
```
This will:
1. notice the images are missing,
2. re-load them from the cached tarball `~/Projects/tp-images.tar.gz`
   (re-downloads the 0.7 GB file once from the GitHub Release if it was deleted),
3. `docker compose up -d` and report the status.

Go back to **Section 1** to verify. Because Postgres data was pruned, the DB starts fresh:
Flyway recreates the schema, instruments re-seed, and a new user/order is created again.

---

## Cheat sheet

```bash
# start / stop / wipe
tp-up.sh                                      # up (restores images)
docker compose -f ~/../docker-compose.yml down          # stop, keep data
docker system prune -a -f --volumes           # wipe everything

# inspect
docker compose -f ~/../docker-compose.yml ps            # health
docker ps                                        # ids/ports
docker exec -it <container> sh            # shell

# kafka
... kafka-topics.sh ... --list               # topics
... kafka-console-consumer.sh ... --topic trade-events --from-beginning
... kafka-consumer-groups.sh ... --all-groups --describe

# db
docker exec -it trade-platform-repo-postgres-1 psql -U trade_app -d trade_platform
```