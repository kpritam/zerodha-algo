package dev.kpritam.zerodha.strategies.everyday

import zio.*
import dev.kpritam.zerodha.kite.time.indiaZone

import java.time.DayOfWeek

private def isThursday =
  Clock.instant.map(_.atZone(indiaZone).getDayOfWeek == DayOfWeek.THURSDAY)

private def findMinBy(items: List[Double], value: Double) =
  for thursday <- isThursday
  yield items.minBy(i => if thursday && i > value then Double.MaxValue else math.abs(i - value))
