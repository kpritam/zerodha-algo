create table instrument
(
    id               integer primary key,
    created_at       datetime default (datetime('now', 'localtime')),
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

create index idx_exchange_instruments on instrument (exchange);

create index idx_trading_symbol_instruments on instrument (trading_symbol);
