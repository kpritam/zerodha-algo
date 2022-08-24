CREATE TABLE IF NOT EXISTS "order"
(
    account_id                text,
    order_id                  text PRIMARY KEY,
    exchange_order_id         text,
    parent_order_id           text,
    status                    text,
    status_message            text,
    order_timestamp           datetime,
    exchange_update_timestamp datetime,
    exchange_timestamp        datetime,
    variety                   text,
    exchange                  text,
    trading_symbol            text,
    instrument_token          integer,
    order_type                text,
    transaction_type          text,
    validity                  text,
    product                   text,
    quantity                  real,
    disclosed_quantity        real,
    price                     real,
    trigger_price             real,
    average_price             real,
    filled_quantity           real,
    pending_quantity          real,
    tag                       text,
    guid                      text,
    validity_ttl              integer
);

CREATE INDEX IF NOT EXISTS idx_exchange_orders ON "order" (exchange);

CREATE INDEX IF NOT EXISTS idx_trading_symbol_orders ON "order" (trading_symbol);
