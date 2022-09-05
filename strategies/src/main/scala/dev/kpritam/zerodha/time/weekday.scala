package dev.kpritam.zerodha.time

import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.kite.time.calendarInstance
import dev.kpritam.zerodha.kite.time.toIndiaLocalDate

import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

def nextWeekday(nextDayOfWeek: Int): LocalDate =
  nextWeekday(nextDayOfWeek, LocalDate.now(indiaZone), calendarInstance)

def nextWeekday(nextDayOfWeek: Int, now: LocalDate, cal: Calendar): LocalDate =
  cal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
  val curWeek  = cal.getTime.toIndiaLocalDate
  cal.add(Calendar.WEEK_OF_YEAR, 1)
  val nextWeek = cal.getTime.toIndiaLocalDate
  if now.isEqual(curWeek) || now.isBefore(curWeek) then curWeek else nextWeek
