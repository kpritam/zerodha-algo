package dev.kpritam.zerodha.strategies

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.{Instrument, InstrumentRequest}
import dev.kpritam.zerodha.strategies.everyday.instrumentRequest
import dev.kpritam.zerodha.time.indiaZone
import zio.*

import java.time.LocalDateTime

def seedInstrumentsIfNeeded(request: InstrumentRequest) =
  for
    instruments <- Instruments.all
    _           <- ZIO.unless(isAfter(instruments, 9, 15))(
                     ZIO.logDebug("Downloading instruments ...") *> KiteService.seedInstruments(request)
                   )
  yield ()

private def isAfter(instruments: List[Instrument], hour: Int, min: Int) =
  instruments.exists { instrument =>
    val time = LocalDateTime.now(indiaZone).withHour(hour).withMinute(min)
    instrument.createdAt.isAfter(time)
  }
