package dev.kpritam.commons.time

import zio.*

import java.time.{LocalDate, ZoneId}
import java.util.{Calendar, Date, TimeZone}

case class TimeServiceLive(clock: Clock, zoneId: ZoneId) extends TimeService:
  export clock.*

  def date: UIO[Date] = instant.map(Date.from)

  def dateAt(hours: Int, minutes: Int): UIO[Date] =
    ZIO.succeed {
      val cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId))
      cal.set(Calendar.HOUR_OF_DAY, hours)
      cal.set(Calendar.MINUTE, minutes)
      cal.getTime
    }

  def localDate: UIO[LocalDate] = clock.localDateTime.map(_.toLocalDate)
