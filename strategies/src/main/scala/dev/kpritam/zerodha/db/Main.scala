//package dev.kpritam.zerodha.db
//
//import com.zerodhatech.models.Order as ZOrder
//import dev.kpritam.zerodha.kite.models.Order
//import zio.*
//
//import java.time.Instant
//import java.util.Date
//
//object Main extends ZIOAppDefault:
//  private val i = Order(
//    accountId = "NUF540",
//    orderId = "220712600450230",
//    status = "PUT ORDER REQ RECEIVED",
//    orderTimestamp = Some(Date.from(Instant.now())),
//    variety = "regular",
//    exchange = "NFO",
//    tradingSymbol = "NIFTY2271415800PE",
//    orderType = "MARKET",
//    transactionType = "SELL",
//    validity = "DAY",
//    product = "NRML",
//    quantity = 50,
//    filledQuantity = "0",
//    pendingQuantity = "50",
//    guid = "48486Xp7l1bW4ESiqI"
//  )
//
//  def run: ZIO[Any, Any, Any] =
//    program.provide(Orders.live)
//
//  val program: ZIO[Orders, Exception, Unit] = for
//    _ <- Console.printLine(i)
//    _ <- Orders.create(i)
//  yield ()
