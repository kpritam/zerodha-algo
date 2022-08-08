package dev.kpritam.zerodha.cron

import zio.*

def everyday(hour: Int, min: Int) =
  Schedule.hourOfDay(hour) && Schedule.minuteOfHour(min)

def onceDay(hour: Int, min: Int) =
  Schedule.hourOfDay(hour) && Schedule.minuteOfHour(min) && Schedule.once
