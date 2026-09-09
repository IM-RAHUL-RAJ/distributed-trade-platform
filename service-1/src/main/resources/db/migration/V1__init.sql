-- =====================================================================
-- Trade Platform - Initial Schema (V1)
-- Managed by Flyway (runs on Service 1 startup)
-- Embedded H2 (PostgreSQL compatibility mode)
-- =====================================================================

-- ---------------------------------------------------------------------
-- users (auth + identity). Auth storage lives in the BFF's own embedded
-- store; this table keeps the shared user-id namespace for FK integrity.
-- Service 1/2 only ever key on user_id and never read this table.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- refresh_tokens (owned by NestJS auth, kept for schema parity)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id     UUID NOT NULL,
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------------
-- accounts (owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE accounts (
    id          UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id     UUID NOT NULL,
    cash        NUMERIC(19,4) NOT NULL DEFAULT 1000000.0000,
    margin      NUMERIC(19,4) NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_accounts_user UNIQUE (user_id),
    CONSTRAINT chk_accounts_cash_non_negative CHECK (cash >= 0)
);

CREATE INDEX idx_accounts_user ON accounts(user_id);

-- ---------------------------------------------------------------------
-- customer_preferences (owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE customer_preferences (
    id                   UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id              UUID NOT NULL,
    trading_experience   VARCHAR(40),
    risk_tolerance       VARCHAR(40),
    trading_style        VARCHAR(40),
    investment_horizon   VARCHAR(40),
    preferred_sectors    VARCHAR(500),
    trading_frequency    VARCHAR(40),
    is_completed         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_preferences_user UNIQUE (user_id)
);

-- ---------------------------------------------------------------------
-- instruments (seed data)
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
-- orders (owned by Service 1 + executed by Service 2)
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

CREATE INDEX idx_orders_user       ON orders(user_id);
CREATE INDEX idx_orders_account    ON orders(account_id);
CREATE INDEX idx_orders_instrument ON orders(instrument_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_orders_created    ON orders(created_at DESC);

-- ---------------------------------------------------------------------
-- trades / positions / transactions are OWNED BY SERVICE 2.
-- Service 1 no longer creates these tables; it reads execution state
-- over Service 2's internal read API (/api/internal) for the dashboard.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- watchlists
-- ---------------------------------------------------------------------
CREATE TABLE watchlists (
    id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id        UUID NOT NULL,
    instrument_id  UUID NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watchlists_user_instrument UNIQUE (user_id, instrument_id)
);

-- ---------------------------------------------------------------------
-- market_data (latest normalized price point per instrument)
-- ---------------------------------------------------------------------
CREATE TABLE market_data (
    instrument_id   UUID PRIMARY KEY REFERENCES instruments(id) ON DELETE CASCADE,
    symbol          VARCHAR(20) NOT NULL,
    price           NUMERIC(19,4) NOT NULL,
    change          NUMERIC(19,4) NOT NULL DEFAULT 0,
    change_percent  NUMERIC(10,4) NOT NULL DEFAULT 0,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_data_symbol ON market_data(symbol);

-- ---------------------------------------------------------------------
-- processed_events (idempotency ledger owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE processed_events (
    event_id      VARCHAR(100) PRIMARY KEY,
    event_type    VARCHAR(40)  NOT NULL,
    order_id      UUID,
    payload       TEXT,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- latest_events (read-state / audit owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE latest_events (
    id            UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    event_type    VARCHAR(40) NOT NULL,
    order_id      UUID,
    payload       CLOB,
    received_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_latest_events_type ON latest_events(event_type, received_at DESC);

-- ---------------------------------------------------------------------
-- order_placed_outbox (transactional outbox owned by Service 1)
-- Rows are written in the SAME DB transaction as the order row, then a
-- scheduled publisher forwards them to Kafka topic 'order-placed'.
-- ---------------------------------------------------------------------
CREATE TABLE order_placed_outbox (
    id             UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    order_id       UUID NOT NULL,
    event_id       UUID NOT NULL,
    payload        CLOB NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PUBLISHED')),
    published_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_status ON order_placed_outbox(status, created_at);