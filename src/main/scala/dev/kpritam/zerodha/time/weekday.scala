package dev.kpritam.zerodha.time

import java.time.LocalDate
import java.util.Calendar

def nextWeekday(nextDayOfWeek: Int): LocalDate =
  val cal = Calendar.getInstance()
  cal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
  cal.add(Calendar.WEEK_OF_YEAR, 1)
  cal.getTime.toIndiaLocalDate
