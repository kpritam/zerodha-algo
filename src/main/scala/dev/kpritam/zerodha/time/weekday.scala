package dev.kpritam.zerodha.time

import java.time.{LocalDate, ZoneId}
import java.util.Calendar

def nextWeekday(nextDayOfWeek: Int): LocalDate =
  val cal      = Calendar.getInstance()
  cal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
  val curWeek  = cal.getTime.toIndiaLocalDate
  cal.add(Calendar.WEEK_OF_YEAR, 1)
  val nextWeek = cal.getTime.toIndiaLocalDate
  val now      = LocalDate.now(indiaZone)
  if now.isBefore(curWeek) then curWeek else nextWeek
