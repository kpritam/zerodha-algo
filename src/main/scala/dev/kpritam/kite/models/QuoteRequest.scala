package dev.kpritam.kite.models

import dev.kpritam.kite.models.QuoteRequest.InstrumentToken

enum QuoteRequest:
  case Instrument(tradingSymbol: TradingSymbol, exchange: Exchange)
  case InstrumentToken(token: String)

  def instrument: String =
    this match
      case Instrument(s, e)   => s"$s:$e"
      case InstrumentToken(t) => t

object QuoteRequest:
  def from(instrument: String): QuoteRequest =
    instrument.split(':') match
      case Array(tradingSymbol, exchange) => Instrument(TradingSymbol(tradingSymbol), Exchange(exchange))
      case Array(token)                   => InstrumentToken(token)
