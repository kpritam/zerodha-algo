package dev.kpritam.commons.time

import zio.*

import java.time.LocalDate
import java.util.Date

trait TimeService extends Clock:
  def date: UIO[Date]
  def dateAt(hours: Int, minutes: Int): UIO[Date]
  def localDate: UIO[LocalDate]
