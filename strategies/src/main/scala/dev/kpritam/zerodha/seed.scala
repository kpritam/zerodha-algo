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
    _           <- (ZIO.logDebug("Downloading instruments ...") *> KiteService.seedAllInstruments(expiryDate))
                     .unless(isAfter(instruments, 9, 15))
  yield ()

private def isAfter(instruments: List[Instrument], hour: Int, min: Int) =
  instruments.exists { instrument =>
    val time = LocalDateTime.now(indiaZone).withHour(hour).withMinute(min)
    instrument.createdAt.isAfter(time)
  }
