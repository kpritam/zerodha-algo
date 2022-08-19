package dev.kpritam.zerodha.strategies.everyday

import com.zerodhatech.models.User
import dev.kpritam.zerodha.cron.*
import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.kite.*
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.models.LastPriceExceeds
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*
import zio.stream.ZStream.HaltStrategy
import zio.stream.*

import java.time.DayOfWeek

trait EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): Task[Unit]

  def modifyPendingOrders: Task[List[String]]
  def closePendingOrders: Task[List[String]]

object EverydayStrategy:
  val live = ZLayer.fromFunction(EverydayStrategyLive.apply)

  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): RIO[EverydayStrategy, Unit] =
    ZIO.serviceWithZIO[EverydayStrategy](_.sellBuyModifyOrder(exchange, name, expiryDay, quantity))

  def modifyPendingOrders: RIO[EverydayStrategy, List[String]] =
    ZIO.serviceWithZIO[EverydayStrategy](_.modifyPendingOrders)

  def closePendingOrders: RIO[EverydayStrategy, List[String]] =
    ZIO.serviceWithZIO[EverydayStrategy](_.closePendingOrders)

case class EverydayStrategyLive(
    kiteClient: KiteClient,
    kiteService: KiteService,
    instruments: Instruments,
    orders: Orders
) extends EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): Task[Unit] =
    for
      state <- State.make

      instrumentRequest = InstrumentRequest(exchange, name, nextWeekday(expiryDay))
      minBy            <- minByBasedOnDayOfWeek(price)
      cepe             <- kiteService.getCEPEInstrument(instrumentRequest, i => minBy(i.lastPrice))
      _                <- ZIO.logInfo(s"Selected instruments: ${cepe.toJson}")

      orderReq = OrderRequest(
                   exchange = exchange.toString,
                   validity = "DAY",
                   product = "NRML",
                   orderType = "MARKET",
                   transactionType = "SELL",
                   quantity = quantity
                 )
      _       <-
        for
          _  <- ZIO.logDebug(s"Subscribing to tokens: ${cepe.tokens.mkString(", ")}")
          f1 <- runOrderCompletionTasks(orderReq, cepe.tokens, state)
                  .haltWhen(ZIO.unit.schedule(onceDay(15, 20)))
                  .runDrain
                  .fork
          _  <- ZIO.sleep(1.seconds)

          // place CE & PE market sell order
          ceOrderReq = orderReq.copy(tradingSymbol = cepe.ce.tradingSymbol)
          peOrderReq = orderReq.copy(tradingSymbol = cepe.pe.tradingSymbol)
          _         <- placeOrder(ceOrderReq, regular, state.updateCe)
          _         <- placeOrder(peOrderReq, regular, state.updatePe)
          _         <- f1.await
        yield ()
    yield ()

  def modifyPendingOrders: Task[List[String]] =
    modifyPendingOrders(o => modifyOrder(OrderRequest.from(o), o))

  def closePendingOrders: Task[List[String]] =
    modifyPendingOrders(o =>
      closeOrder(OrderRequest.marketBuy(o.exchange, o.quantity.toInt, o.tradingSymbol), o)
    )

  def modifyPendingOrders(modify: Order => Task[String]): Task[List[String]] =
    for
      pendingOrders <-
        orders.getPendingSL.tap(o =>
          ZIO.logDebug(s"Modifying pending orders: ${o.map(_.orderId).mkString("[", ", ", "]")}")
        )
      orders        <- kiteClient.getOrders(pendingOrders.map(_.orderId))
      res           <-
        ZIO.foreachPar(orders)(o =>
          modify(o).tapError(e => ZIO.logError(e.getMessage)).orElseSucceed(s"FAILED_${o.orderId}")
        )
    yield res

  private def runOrderCompletionTasks(
      orderReq: OrderRequest,
      tokens: List[java.lang.Long],
      state: Ref[State]
  ) =
    kiteService
      .subscribeOrders(tokens)
      .collectZIO {
        case o if o.completed =>
          for
            _ <- ZIO.logDebug(s"[1] Order completed: $o")
            _ <- ZIO.sleep(1.second)
            s <- state.get.tap(s => ZIO.logDebug("[2] Current State: " + s))
            _ <- ZIO.whenCase(Some(o.orderId)) {
                   case s.ceOrderId => placeSLOrder(orderReq, o, state.updateCeSL).ignore
                   case s.peOrderId => placeSLOrder(orderReq, o, state.updatePeSL).ignore

                   case s.ceSLOrderId if s.peOrder.nonEmpty =>
                     modifyOrder(orderReq, s.peOrder.get).ignore
                   case s.peSLOrderId if s.ceOrder.nonEmpty =>
                     modifyOrder(orderReq, s.ceOrder.get).ignore
                 }
          yield ()
      }
      .take(4)

  private def placeSLOrder(orderReq: OrderRequest, order: Order, update: Order => UIO[Unit]) =
    (for
      _          <- ZIO.logDebug(s"[PlaceSLOrder] Placing order: $orderReq")
      quote      <- kiteClient.getQuote(QuoteRequest.from(order))
      (tp, price) = triggerPriceAndPrice(order.averagePrice, quote)
      res        <-
        kiteService
          .placeOrder(orderReq.toSLBuy(tp, price, order.tradingSymbol), regular)
          .tap(update)
      _          <- ZIO.logDebug(s"[PlaceSLOrder] Order placed: $res")
    yield res).tapError(e =>
      ZIO.logError(s"[PlaceSLOrder] Failed to place order, reason: ${e.getMessage}")
    )

  private def modifyOrder(orderReq: OrderRequest, order: Order) =
    (for
      _     <- ZIO.logDebug(s"Modifying order: $order")
      quote <- kiteClient.getQuote(QuoteRequest.from(order))
      _     <- ZIO
                 .fail(LastPriceExceeds(quote.lastPrice, order.price))
                 .when(quote.lastPrice * 2.5 > order.price)

      (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
      modifyReq   = orderReq.toSLBuy(tp, price, order.tradingSymbol)
      res        <- kiteClient.modifyOrder(order.orderId, modifyReq, regular)
      _          <- ZIO.logDebug(s"Order modified: $res")
    yield res)
      .tapError(e =>
        ZIO.logError(
          s"Order modification failed: TradingSymbol: ${orderReq.tradingSymbol}, reason : ${e.getMessage}"
        )
      )

  private def closeOrder(orderReq: OrderRequest, order: Order) =
    for
      _   <- ZIO.logDebug(s"Closing order: $order")
      res <- kiteClient.modifyOrder(order.orderId, orderReq, regular)
      _   <- ZIO.logDebug(s"Order closed: $res")
    yield res

  private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
    for
      _   <- ZIO.logDebug(s"Placing order: $orderReq")
      res <- kiteService.placeOrder(orderReq, variety).tap(update)
      _   <- ZIO.logDebug(s"Order placed: $res")
    yield res
