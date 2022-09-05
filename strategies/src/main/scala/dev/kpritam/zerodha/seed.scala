package dev.kpritam.zerodha

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.models.InstrumentsNotFound
import dev.kpritam.zerodha.time.nextWeekday
import zio.*

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar

private val expiryDate = nextWeekday(Calendar.THURSDAY)

def seedInstrumentsIfNeeded =
  for
    _ <- ZIO.logDebug(s"Downloading instruments expiring at $expiryDate...")
    r <- KiteService
           .seedAllInstruments(expiryDate)
           .filterOrDie(_.nonEmpty)(InstrumentsNotFound(expiryDate))
    _ <- ZIO.logDebug(s"Total: ${r.size} instruments downloaded and stored in DB")
  yield ()
