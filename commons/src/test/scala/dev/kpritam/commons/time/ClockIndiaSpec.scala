package dev.kpritam.commons.time

import zio.*
import zio.test.*

object ClockIndiaSpec extends ZIOSpecDefault:
  def spec =
    test("india clock") {
      for dt <- clockIndia.localDateTime
      yield assertTrue(true)
    }
