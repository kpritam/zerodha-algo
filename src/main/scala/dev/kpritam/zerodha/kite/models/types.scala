package dev.kpritam.zerodha.kite.models

import zio.json.*

opaque type Exchange = String
object Exchange:
  def apply(exchange: String): Exchange = exchange

opaque type TradingSymbol = String
object TradingSymbol:
  def apply(symbol: String): TradingSymbol = symbol

opaque type InstrumentToken = String

type Variety   = "regular" | "amo" | "co" | "iceberg"
type OrderType = "MARKET" | "LIMIT" | "SL" | "SL-M"
type Product   = "CNC" | "NRML" | "MIS"
type Validity  = "DAY" | "IOC" | "TTL"
