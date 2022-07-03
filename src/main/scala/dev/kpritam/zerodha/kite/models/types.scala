package dev.kpritam.zerodha.kite.models

opaque type Exchange = String
object Exchange:
  def apply(exchange: String): Exchange = exchange

opaque type TradingSymbol = String
object TradingSymbol:
  def apply(symbol: String): TradingSymbol = symbol

opaque type InstrumentToken = String
