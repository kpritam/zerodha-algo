CREATE TABLE IF NOT EXISTS instrument
(
    id               integer PRIMARY KEY,
    created_at       datetime DEFAULT (datetime('now', 'localtime')),
    exchange         text,
    trading_symbol   text,
    expiry           datetime,
    instrument_token integer,
    exchange_token   integer,
    name             text,
    last_price       real,
    strike           real,
    tick_size        real,
    lot_size         integer,
    instrument_type  text,
    segment          text
);

CREATE INDEX IF NOT EXISTS idx_exchange_instruments ON instrument (exchange);

CREATE INDEX IF NOT EXISTS idx_trading_symbol_instruments ON instrument (trading_symbol);
