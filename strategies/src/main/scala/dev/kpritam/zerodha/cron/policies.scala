package dev.kpritam.zerodha.cron

import zio.*

val everyMorning9_30 = everyDay(0, 9)
val everyNoon1_30    = everyDay(0, 10)
val everyNoon2_30    = everyDay(0, 11)

def everyDay(hour: Int, min: Int) =
  Schedule.hourOfDay(hour) && Schedule.minuteOfHour(min)
