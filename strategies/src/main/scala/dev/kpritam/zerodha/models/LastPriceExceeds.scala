package dev.kpritam.zerodha.models

case class LastPriceExceeds(instrumentLastPrice: Double, previousPrice: Double) extends Throwable
