package dev.kpritam.zerodha

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.time.nextWeekday
import zio.*

import java.time.{LocalDate, LocalDateTime}
import java.util.Calendar

private val expiryDate = nextWeekday(Calendar.THURSDAY)

case class InstrumentsNotFound(expiryDate: LocalDate) extends Throwable {
  override def getMessage: String = s"Instruments not found for expiry date of: $expiryDate"
}

def seedInstrumentsIfNeeded =
  for
    _ <- ZIO.logDebug(s"Downloading instruments expiring at $expiryDate...")
    r <- KiteService
           .seedAllInstruments(expiryDate)
           .filterOrDie(_.nonEmpty)(InstrumentsNotFound(expiryDate))
    _ <- ZIO.logDebug(s"Total: ${r.size} instruments downloaded and stored in DB")
  yield ()
