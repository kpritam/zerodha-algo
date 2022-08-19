package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.InstrumentRequest
import dev.kpritam.zerodha.kite.time.indiaZone
import zio.*

import java.time.DayOfWeek

private def isThursday =
  Clock.instant.map(_.atZone(indiaZone).getDayOfWeek == DayOfWeek.THURSDAY)

private def minByBasedOnDayOfWeek(price: Double): UIO[Double => Double] =
  for thursday <- isThursday
  yield lastPrice =>
    if thursday && lastPrice > price then Double.MaxValue else math.abs(lastPrice - price)
