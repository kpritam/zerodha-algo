package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.LTPQuote

extension (quotes: Map[QuoteRequest, LTPQuote])
  def getLastPrice(instrumentToken: Long): Double =
    quotes.get(QuoteRequest.InstrumentToken(instrumentToken)).map(_.lastPrice).getOrElse(0)
