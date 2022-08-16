package dev.kpritam.zerodha.models

import java.time.LocalDate

case class LastPriceExceeds(instrumentLastPrice: Double, previousPrice: Double) extends Throwable

case class InstrumentsNotFound(expiryDate: LocalDate) extends Throwable {
  override def getMessage: String = s"Instruments not found for expiry date of: $expiryDate"
}
