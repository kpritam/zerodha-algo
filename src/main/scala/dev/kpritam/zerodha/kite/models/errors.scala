package dev.kpritam.zerodha.kite.models

case class InvalidInstrumentToken(token: String) extends Throwable

case class QuoteNoteFound(instrument: String) extends Throwable

case class LastPriceExceeds(instrumentLastPrice: Double, previousPrice: Double) extends Throwable
