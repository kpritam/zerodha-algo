package dev.kpritam.zerodha.time

import java.util.{Calendar, Date}

def nextWeekday(nextDayOfWeek: Int): Date =
  val cal = Calendar.getInstance()
  cal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  cal.add(Calendar.WEEK_OF_YEAR, 1)
  cal.getTime
