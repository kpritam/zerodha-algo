package dev.kpritam.zerodha

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.time.nextWeekday
import zio.*

import java.time.LocalDateTime
import java.util.Calendar

private val expiryDate = nextWeekday(Calendar.THURSDAY)

def seedInstrumentsIfNeeded =
  for
    instruments <- Instruments.all
    _           <- ZIO.unless(isAfter(instruments, 9, 20)) {
                     for
                       _ <- ZIO.logDebug(s"Downloading instruments expiring at $expiryDate...")
                       r <- KiteService.seedAllInstruments(expiryDate)
                       _ <- ZIO.logDebug(s"Total: ${r.size} instruments downloaded and stored in DB")
                     yield ()
                   }
  yield ()

private def isAfter(instruments: List[Instrument], hour: Int, min: Int) =
  instruments.exists { instrument =>
    val time = LocalDateTime.now(indiaZone).withHour(hour).withMinute(min)
    instrument.createdAt.isAfter(time)
  }
