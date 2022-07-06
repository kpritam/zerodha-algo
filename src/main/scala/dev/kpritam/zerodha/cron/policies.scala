package dev.kpritam.zerodha.cron

import zio.*

val everyMorning9_30 = everyDay(9, 30)
val everyNoon1_30    = everyDay(13, 30)
val everyNoon2_30    = everyDay(14, 30)

def everyDay(hour: Int, min: Int) =
  Schedule.hourOfDay(hour) && Schedule.minuteOfHour(min)
