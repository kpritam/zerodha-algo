package dev.kpritam.zerodha.utils

import com.zerodhatech.models.LTPQuote
import dev.kpritam.zerodha.kite.models.QuoteRequest
import zio.ZIO

extension (quotes: Map[QuoteRequest, LTPQuote])
  def getLastPriceOrZero(instrumentToken: Long): Double =
    quotes.get(QuoteRequest.InstrumentToken(instrumentToken)).map(_.lastPrice).getOrElse(0)

extension [R, E <: Throwable, A](zio: ZIO[R, E, A])
  def catchAndLog(msg: String) =
    zio.catchAll(e => ZIO.logError(s"$msg: ${e.getMessage}").unit)
