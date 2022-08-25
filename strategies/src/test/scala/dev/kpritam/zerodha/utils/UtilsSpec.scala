package dev.kpritam.zerodha.utils

import com.zerodhatech.models.Quote
import zio.*
import zio.test.*

object UtilsSpec extends ZIOSpecDefault:
  private def quote(upperCircuitLimit: Double) =
    val q = Quote()
    q.upperCircuitLimit = upperCircuitLimit
    q

  def spec =
    suite("trigger price")(
      test("upperCircuitLimit < triggerPrice") {
        val (tp, p) = triggerPriceAndPrice(12.4, quote(30.0))
        assertTrue(tp == 31.0) && assertTrue(p == 30.0)
      },
      test("upperCircuitLimit > triggerPrice") {
        val (tp, p) = triggerPriceAndPrice(12.4, quote(31.5))
        assertTrue(tp == 31.0) && assertTrue(p == 31.1)
      },
      test("upperCircuitLimit == triggerPrice") {
        val (tp, p) = triggerPriceAndPrice(12.4, quote(31.0))
        assertTrue(tp == 31.0) && assertTrue(p == 31.0)
      }
    )
