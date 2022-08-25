package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Order
import zio.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zio.test.*

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

val order1 = Order(
  accountId = "NUF540",
  orderId = "1",
  status = "PUT ORDER REQ RECEIVED",
  orderTimestamp = Some(Date.from(Instant.now())),
  variety = "regular",
  exchange = "NFO",
  tradingSymbol = "NIFTY2271415800PE",
  orderType = "MARKET",
  transactionType = "SELL",
  validity = "DAY",
  product = "NRML",
  quantity = 50.0,
  filledQuantity = "0.0",
  pendingQuantity = "50.0",
  guid = "48486Xp7l1bW4ESiqI"
)

val order2 = order1.copy(orderId = "2", status = "TRIGGER PENDING", orderType = "SL")
val order3 = order1.copy(
  orderId = "3",
  status = "TRIGGER PENDING",
  orderType = "SL",
  orderTimestamp = Some(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
)

object OrderSpec extends ZIOSpecDefault:
  def spec = {
    test("Order Repo - CRUD")(
      for
        _               <- ZIO.foreachParDiscard(List(order1, order2, order3))(Orders.create)
        actualOrder1    <- Orders.get(order1.orderId).some
        actualPendingSL <- Orders.getPendingSL
      yield assertTrue(actualOrder1 == order1) && assert(actualPendingSL)(
        hasSameElements(List(order2, order3))
      )
    ) @@ before(Migrations.reset)
  }.provideShared(QuillCtx.dataSourceLayer, Migrations.live, OrdersLive.layer)

@main def main() =
  println(Date.from(Instant.now()).toInstant.toString.replaceAll("T.*Z", ""))
