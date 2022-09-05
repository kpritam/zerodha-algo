package dev.kpritam.commons.time

import zio.*

import java.time
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}
import java.util.concurrent.TimeUnit

private val indiaZone = ZoneId.of("Asia/Kolkata")

val clockIndia = Clock.ClockJava(time.Clock.system(indiaZone))
