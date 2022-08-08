package dev.kpritam.zerodha.time

import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.kite.time.toIndiaLocalDate

import java.time.LocalDate
import java.time.ZoneId
import java.util.{Calendar, TimeZone}

def nextWeekday(nextDayOfWeek: Int): LocalDate =
  val cal      = Calendar.getInstance(TimeZone.getTimeZone(indiaZone))
  cal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
  val curWeek  = cal.getTime.toIndiaLocalDate
  cal.add(Calendar.WEEK_OF_YEAR, 1)
  val nextWeek = cal.getTime.toIndiaLocalDate
  val now      = LocalDate.now(indiaZone)
  if now.isEqual(curWeek) || now.isBefore(curWeek) then curWeek else nextWeek
