package dev.kpritam.zerodha

import com.zerodhatech.models.LTPQuote
import dev.kpritam.zerodha.kite.models.QuoteRequest

extension (quotes: Map[QuoteRequest, LTPQuote])
  def getLastPriceOrZero(instrumentToken: Long): Double =
    quotes.get(QuoteRequest.InstrumentToken(instrumentToken)).map(_.lastPrice).getOrElse(0)
