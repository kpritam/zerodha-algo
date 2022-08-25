package dev.kpritam.zerodha.time

import dev.kpritam.zerodha.kite.time.indiaZone
import zio.*
import zio.test.*

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

private case class TestCalendar(cal: Calendar, now: LocalDate)

private def calendar(year: Int, month: Int, date: Int) =
  val now = LocalDateTime.of(year, month, date, 0, 0, 0, 0).atZone(indiaZone)
  val cal = Calendar.getInstance(TimeZone.getTimeZone(indiaZone))
  cal.setTime(Date.from(Instant.from(now)))
  TestCalendar(cal, now.toLocalDate)

object NextWeekdaySpec extends ZIOSpecDefault:
  private val currWeekThursday = LocalDate.of(2022, 8, 18)
  private val nextWeekThursday = LocalDate.of(2022, 8, 25)

  private val testData = List(
    ("Monday", calendar(2022, 8, 15), currWeekThursday),
    ("Tuesday", calendar(2022, 8, 16), currWeekThursday),
    ("Wednesday", calendar(2022, 8, 17), currWeekThursday),
    ("Thursday", calendar(2022, 8, 18), currWeekThursday),
    ("Friday", calendar(2022, 8, 19), nextWeekThursday),
    ("Saturday", calendar(2022, 8, 20), nextWeekThursday),
    ("Sunday", calendar(2022, 8, 21), nextWeekThursday)
  )

  def spec =
    suite("nextWeekday - Thursday")(
      testData.map { case (name, TestCalendar(cal, now), expected) =>
        test(s"day of week: $name") {
          val result = nextWeekday(Calendar.THURSDAY, now, cal)
          assertTrue(result == expected)
        }
      }
    )
