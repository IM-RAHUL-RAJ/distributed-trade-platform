-- =====================================================================
-- Trade Platform - Initial Schema (V1)
-- Managed by Flyway (runs on Service 1 startup)
-- PostgreSQL 16
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
-- users (auth + identity, owned for auth by NestJS, used by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- refresh_tokens (owned by NestJS auth)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------------
-- accounts (owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cash        NUMERIC(19,4) NOT NULL DEFAULT 1000000.0000,
    margin      NUMERIC(19,4) NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_accounts_user UNIQUE (user_id),
    CONSTRAINT chk_accounts_cash_non_negative CHECK (cash >= 0)
);

CREATE INDEX idx_accounts_user ON accounts(user_id);

-- ---------------------------------------------------------------------
-- customer_preferences (owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE customer_preferences (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trading_experience   VARCHAR(40),
    risk_tolerance       VARCHAR(40),
    trading_style        VARCHAR(40),
    investment_horizon   VARCHAR(40),
    preferred_sectors    VARCHAR(500),
    trading_frequency    VARCHAR(40),
    is_completed         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_preferences_user UNIQUE (user_id)
);

-- ---------------------------------------------------------------------
-- instruments (seed data)
-- ---------------------------------------------------------------------
CREATE TABLE instruments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
    updated_at      TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_instruments_symbol ON instruments(symbol);

-- ---------------------------------------------------------------------
-- orders (owned by Service 1 + executed by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
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
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user       ON orders(user_id);
CREATE INDEX idx_orders_account    ON orders(account_id);
CREATE INDEX idx_orders_instrument ON orders(instrument_id);
CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_orders_created    ON orders(created_at DESC);

-- ---------------------------------------------------------------------
-- trades (owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE trades (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL REFERENCES orders(id),
    user_id        UUID NOT NULL REFERENCES users(id),
    account_id     UUID NOT NULL REFERENCES accounts(id),
    instrument_id  UUID NOT NULL REFERENCES instruments(id),
    symbol         VARCHAR(20) NOT NULL,
    side           VARCHAR(4) NOT NULL CHECK (side IN ('BUY','SELL')),
    quantity       INTEGER NOT NULL CHECK (quantity > 0),
    price          NUMERIC(19,4) NOT NULL,
    executed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trades_order UNIQUE (order_id)
);

CREATE INDEX idx_trades_user     ON trades(user_id);
CREATE INDEX idx_trades_executed ON trades(executed_at DESC);

-- ---------------------------------------------------------------------
-- positions (owned by Service 2)
-- ---------------------------------------------------------------------
CREATE TABLE positions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id),
    account_id     UUID NOT NULL REFERENCES accounts(id),
    instrument_id  UUID NOT NULL REFERENCES instruments(id),
    symbol         VARCHAR(20) NOT NULL,
    quantity       INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    average_price  NUMERIC(19,4) NOT NULL DEFAULT 0,
    realized_pnl   NUMERIC(19,4) NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_positions_user_instrument UNIQUE (user_id, instrument_id)
);

CREATE INDEX idx_positions_account ON positions(account_id);

-- ---------------------------------------------------------------------
-- transactions (owned by Service 2, cash-flow audit)
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id),
    account_id     UUID NOT NULL REFERENCES accounts(id),
    order_id       UUID REFERENCES orders(id),
    trade_id       UUID REFERENCES trades(id),
    type           VARCHAR(30) NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    balance_after  NUMERIC(19,4) NOT NULL,
    description    VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user    ON transactions(user_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_created ON transactions(created_at DESC);

-- ---------------------------------------------------------------------
-- watchlists
-- ---------------------------------------------------------------------
CREATE TABLE watchlists (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    instrument_id  UUID NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
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
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- latest_events (read-state / audit owned by Service 1)
-- ---------------------------------------------------------------------
CREATE TABLE latest_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    VARCHAR(40) NOT NULL,
    order_id      UUID,
    payload       JSONB,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_latest_events_type ON latest_events(event_type, received_at DESC);