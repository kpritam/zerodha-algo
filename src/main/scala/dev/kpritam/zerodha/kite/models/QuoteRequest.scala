package dev.kpritam.zerodha.kite.models

import dev.kpritam.zerodha.kite.models.Order
import QuoteRequest.InstrumentToken
import zio.*

enum QuoteRequest:
  case Instrument(tradingSymbol: TradingSymbol, exchange: Exchange)
  case InstrumentToken(token: Long)

  def instrument: String =
    this match
      case Instrument(s, e)   => s"$e:$s"
      case InstrumentToken(t) => t.toString

object QuoteRequest:
  def from(instrument: String): IO[InvalidInstrumentToken, QuoteRequest] =
    instrument.split(':') match
      case Array(tradingSymbol, exchange) =>
        ZIO.succeed(Instrument(TradingSymbol(tradingSymbol), Exchange(exchange)))
      case Array(token)                   => ZIO.succeed(InstrumentToken(token.toLong))
      case _                              => ZIO.fail(InvalidInstrumentToken(instrument))

  def from(order: Order): QuoteRequest =
    Instrument(TradingSymbol(order.tradingSymbol), Exchange(order.exchange))
