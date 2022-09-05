package dev.kpritam.zerodha.kite.time

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util.{Calendar, Date, TimeZone}

val indiaZone = ZoneId.of("Asia/Kolkata")

def calendarInstance: Calendar =
  Calendar.getInstance(TimeZone.getTimeZone(indiaZone))

def dateNow()     = LocalDate.now(indiaZone)
def dateTimeNow() = LocalDateTime.now(indiaZone)

def todayAt(hours: Int, minutes: Int): Date =
  val cal = calendarInstance
  cal.set(Calendar.HOUR_OF_DAY, hours)
  cal.set(Calendar.MINUTE, minutes)
  cal.getTime

extension (date: Date) def toIndiaLocalDate = date.toInstant.atZone(indiaZone).toLocalDate
