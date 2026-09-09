-- =====================================================================
-- Trade Platform - Service 2 Execution Schema (V1)
-- Managed by Flyway (runs on Service 2 startup)
-- Service 2 owns its own H2 file DB: /data/s2_trade_platform
-- This is the authoritative source of execution state (cash, positions,
-- trades, transactions, market_data used for fill prices, Kafka acks).
-- Service 1 owns a separate read model DB and never shares this one.
-- =====================================================================

-- ---------------------------------------------------------------------
-- instruments (reference data, duplicated from Service 1's instruments
-- so Service 2's execution pricing is fully self-contained)
-- ---------------------------------------------------------------------
CREATE TABLE instruments (
    id              UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    symbol          VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    exchange        VARCHAR(20) NOT NULL,
    currency        VARCHAR(8)  NOT NULL DEFAULT 'USD',
    type            VARCHAR(30) NOT NULL DEFAULT 'EQUITY',
    tick_size       NUMERIC(10,4) NOT NULL DEFAULT 0.01,
    lot_size        INTEGER NOT NULL DEFAULT 1,
    last_price      NUMERIC(19,4),
    change          NUMERIC(19,4) DEFAULT 0,
    change_percent  NUMERIC(10,4) DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_instruments_symbol ON instruments(symbol);

-- ---------------------------------------------------------------------
-- accounts (execution cash ledger, owned by Service 2). Lazily seeded
-- from the first order-placed event; keeps Service 1's account id.
-- ---------------------------------------------------------------------
CREATE TABLE accounts (
    id          UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id     UUID NOT NULL,
    cash        NUMERIC(19,4) NOT NULL DEFAULT 1000000.0000,
    margin      NUMERIC(19,4) NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_s2_accounts_user UNIQUE (user_id),
    CONSTRAINT chk_s2_accounts_cash_non_negative CHECK (cash >= 0)
);

CREATE INDEX idx_s2_accounts_user ON accounts(user_id);

-- ---------------------------------------------------------------------
-- orders (execution copy, owned by Service 2). Materialized from the
-- order-placed event because it is stored transactionally in the outbox.
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id               UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id          UUID NOT NULL,
    account_id       UUID NOT NULL REFERENCES accounts(id),
    instrument_id    UUID NOT NULL REFERENCES instruments(id),
    symbol           VARCHAR(20) NOT NULL,
    side             VARCHAR(4) NOT NULL CHECK (side IN ('BUY','SELL')),
    order_type       VARCHAR(10) NOT NULL DEFAULT 'MARKET' CHECK (order_type IN ('MARKET','LIMIT')),
    quantity         INTEGER NOT NULL CHECK (quantity > 0),
    requested_price  NUMERIC(19,4),
    executed_price   NUMERIC(19,4),
    status           VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','EXECUTED','REJECTED','CANCELLED')),
    reject_reason    VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_s2_orders_user       ON orders(user_id);
CREATE INDEX idx_s2_orders_account    ON orders(account_id);
CREATE INDEX idx_s2_orders_status     ON orders(status);

-- ---------------------------------------------------------------------
-- market_data (latest price used for market fills)
-- ---------------------------------------------------------------------
CREATE TABLE market_data (
    instrument_id   UUID PRIMARY KEY REFERENCES instruments(id) ON DELETE CASCADE,
    symbol          VARCHAR(20) NOT NULL,
    price           NUMERIC(19,4) NOT NULL,
    change          NUMERIC(19,4) NOT NULL DEFAULT 0,
    change_percent  NUMERIC(10,4) NOT NULL DEFAULT 0,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_s2_market_data_symbol ON market_data(symbol);

-- ---------------------------------------------------------------------
-- positions (owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE positions (
    id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id        UUID NOT NULL,
    account_id     UUID NOT NULL REFERENCES accounts(id),
    instrument_id  UUID NOT NULL REFERENCES instruments(id),
    symbol         VARCHAR(20) NOT NULL,
    quantity       INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    average_price  NUMERIC(19,4) NOT NULL DEFAULT 0,
    realized_pnl   NUMERIC(19,4) NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_s2_positions_user_instrument UNIQUE (user_id, instrument_id)
);

-- ---------------------------------------------------------------------
-- trades (execution records, owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE trades (
    id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    order_id       UUID NOT NULL REFERENCES orders(id),
    user_id        UUID NOT NULL,
    account_id     UUID NOT NULL REFERENCES accounts(id),
    instrument_id  UUID NOT NULL REFERENCES instruments(id),
    symbol         VARCHAR(20) NOT NULL,
    side           VARCHAR(4) NOT NULL CHECK (side IN ('BUY','SELL')),
    quantity       INTEGER NOT NULL CHECK (quantity > 0),
    price          NUMERIC(19,4) NOT NULL,
    executed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_s2_trades_order UNIQUE (order_id)
);

CREATE INDEX idx_s2_trades_user ON trades(user_id);

-- ---------------------------------------------------------------------
-- transactions (cash-flow audit, owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id        UUID NOT NULL,
    account_id     UUID NOT NULL REFERENCES accounts(id),
    order_id       UUID REFERENCES orders(id),
    trade_id       UUID REFERENCES trades(id),
    type           VARCHAR(30) NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    balance_after  NUMERIC(19,4) NOT NULL,
    description    VARCHAR(500),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_s2_transactions_user ON transactions(user_id);

-- ---------------------------------------------------------------------
-- processed_events (Kafka ack/idempotency ledger, owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE processed_events (
    event_id      VARCHAR(100) PRIMARY KEY,
    event_type    VARCHAR(40)  NOT NULL,
    order_id      UUID,
    payload       TEXT,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);