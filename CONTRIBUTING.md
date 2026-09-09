# Contributing — How to Run & Check the Project

Full microservices trading platform, 100% local: **no Postgres, no cloud, no
Codespaces**. Everything runs in Docker Compose with two embedded H2 databases.

This guide is for anyone who cloned the repo and wants to run it and verify
that it works.

---

## 1. Prerequisites

- **Docker Desktop** (with Docker Compose v2) running.
- **Git.**
- Free host ports: `3000` (BFF), `4200` (frontend), `8080` (mock feed),
  `8081`/`8082` (services), `9092` (Kafka).
- **Disk space.** The build produces large images and the Docker VM raw disk
  grows — keep ~10 GB free and prune after the session (see **Step 8**).

## 2. Clone

```bash
git clone https://github.com/IM-RAHUL-RAJ/distributed-trade-platform.git
cd distributed-trade-platform
```

## 3. (Optional) Configure via a `.env` file

Ports and secrets all have sensible defaults from `docker-compose.yml`. Override
any of them by creating a `.env` next to the compose file:

```dotenv
KAFKA_PORT=9092
SERVICE1_PORT=8081
SERVICE2_PORT=8082
BFF_PORT=3000
FRONTEND_PORT=4200
MOCK_API_PORT=8080
H2_TCP_PORT=9095
S2_H2_TCP_PORT=9096
JWT_SECRET=change-me-to-a-long-random-string
KAFKA_CLUSTER_ID=5L6g3nShT-eMCtK--X86sw
```

## 4. Build & start the stack

```bash
docker compose build
docker compose up -d
```

Or in one shot: `docker compose up -d --build`.

First startup builds 5 images and boots Kafka + 6 containers; give it ~1 minute.

## 5. Verify it's healthy

```bash
docker compose ps
```

Everything should show `healthy` within ~60–90 s:

| Component | Container | Port | Role |
|---|---|---|---|
| Kafka | `trade-platform-kafka-1` | 9092 | message bus |
| Mock feed | `trade-platform-mock-funksance-1` | 8080 | quotes + trades |
| Service 1 | `trade-platform-service-1-1` | 8081 | orders + outbox, own read-model H2 |
| Service 2 | `trade-platform-service-2-1` | 8082 | executor: cash/positions/trades, own H2 |
| BFF | `trade-platform-bff-1` | 3000 | API gateway + auth |
| Frontend | `trade-platform-frontend-1` | 4200 | Angular UI |

Check logs for errors:

```bash
docker compose logs service-1 | tail -50
docker compose logs service-2 | tail -50
```

## 6. Smoke test end to end

Register a user, log in, place a market BUY:

```bash
# register (firstName + lastName need 2+ chars)
curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!","firstName":"Test","lastName":"User"}'

# login -> copy the accessToken
curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.com","password":"Passw0rd!"}'

# place a market BUY (note the returned order id + status PENDING)
curl -s -X POST http://localhost:3000/api/v1/orders \
  -H "Authorization: Bearer <TOKEN>" -H 'Content-Type: application/json' \
  -d '{"symbol":"AAPL","side":"BUY","quantity":10,"orderType":"MARKET"}'
```

Within ~2 seconds the order should go `PENDING → EXECUTED`.

Open the UI: `http://localhost:4200` — dashboard shows cash `$1,000,000`, live
quotes for all 12 instruments, and after the buy: an AAPL position, a trade
row, and a `TRADE_BUY` transaction.

## 7. Check the machinery behind the scenes

### Kafka

```bash
# topics
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list

# events leaving Service 1, live
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic order-placed

# execution results coming back, live (2nd terminal)
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic trade-events

# consumer lag (should be 0 after the order settles)
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-1-group
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group service-2-executor
```

### Databases (each service owns its own H2)

Service 1 read model (port 9095) — helper `tools/h2sh.sh`:

```bash
./tools/h2sh.sh -sql "SELECT symbol, side, quantity, status FROM orders ORDER BY created_at DESC LIMIT 3"
./tools/h2sh.sh -sql "SELECT order_id, status FROM order_placed_outbox ORDER BY created_at DESC LIMIT 3"
```

- `orders.status` should be `EXECUTED`, `order_placed_outbox.status` `PUBLISHED`.

Service 2 execution ledger (port 9096):

```bash
docker compose exec service-2 sh -c 'cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && java -cp h2.jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9096/s2_trade_platform" -user sa -password "" -sql "SELECT id, symbol, side, quantity, status, executed_price FROM orders ORDER BY created_at DESC LIMIT 3"'
```

Expect `EXECUTED`, plus one row per event in the idempotency table:

```bash
docker compose exec service-2 sh -c 'cd /tmp && wget -q https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar && java -cp h2.jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9096/s2_trade_platform" -user sa -password "" -sql "SELECT event_type, order_id, processed_at FROM processed_events ORDER BY processed_at DESC LIMIT 3"'
```

### Service 2 internal read API (what Service 1 uses)

Endpoints take the **userId**. Find it from the login response or S1's orders:

```bash
USER_ID=cc49f3ba-87cc-4446-9e9a-09ccaea1e472   # example user id
curl -s http://localhost:8082/api/internal/accounts/$USER_ID
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/positions
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/trades
curl -s http://localhost:8082/api/internal/accounts/$USER_ID/transactions
```

## 8. Stop & clean up (prevents disk bloat)

```bash
# stop the stack
docker compose down

# reclaim space: drop cached/dangling images + build cache
docker image prune -af
docker builder prune -af

# remove leftover volumes too (also wipes h2data/s2data -> fresh DBs next run)
docker volume prune -f

# last resort if disk is still tight
docker system prune -af --volumes

# verify
docker system df
```

## Troubleshooting

- **Order stays `PENDING` / outbox stuck in `PENDING`** → the `OutboxPublisher`
  didn't run: `docker compose logs service-1 | grep Outbox`, and confirm
  `Service1Application` has `@EnableScheduling`.
- **Cross-DB instrument mismatch after re-seeding** → both
  `V2__seed_instruments.sql` files use the same **deterministic** UUIDs
  (`00000000-0000-0000-0000-000000000001..00c`) — keep them identical.
- **H2 client can't connect** → the TCP ports (9095/9096) are only reachable
  inside the compose network. For a desktop client (DBeaver), temporarily map
  the port (see `how_to_test.md`) or `docker compose` exec the shell as above.
- **Port already in use** → override via `.env` (e.g. `SERVICE1_PORT=8083`).