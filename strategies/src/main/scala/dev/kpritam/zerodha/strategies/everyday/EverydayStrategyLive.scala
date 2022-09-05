package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.cron.*
import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.kite.KiteClient
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.InstrumentRequest
import dev.kpritam.zerodha.kite.models.Order
import dev.kpritam.zerodha.kite.models.OrderRequest
import dev.kpritam.zerodha.kite.models.QuoteRequest
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.models.LastPriceExceeds
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*
import zio.stream.ZStream.HaltStrategy
import zio.stream.*

case class EverydayStrategyLive(
    kiteClient: KiteClient,
    kiteService: KiteService,
    instruments: Instruments,
    orders: Orders
) extends EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      price: Double,
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
            _ <- ZIO.logDebug(s"[1] Order completed: ${o.debug}")
            _ <- ZIO.sleep(1.second)
            s <- state.get.tap(s => ZIO.logDebug(s"[2] Current State: ${s.debug}"))
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

  private def placeSLOrder(orderReq: OrderRequest, order: Order, update: Order => UIO[Unit]) =
    (for
      _             <- ZIO.logDebug(s"[PlaceSLOrder] Getting quote: ${order.tradingSymbol}")
      quote         <- kiteClient.getQuote(QuoteRequest.from(order))
      (tp, price)    = triggerPriceAndPrice(order.averagePrice, quote)
      updatedRequest = orderReq.toSLBuy(tp, price, order.tradingSymbol)
      _             <- ZIO.logDebug(s"[PlaceSLOrder] Placing order: ${updatedRequest.debug}")
      res           <- kiteService.placeOrder(updatedRequest, regular).tap(update)
      _             <- ZIO.logDebug(s"[PlaceSLOrder] Order placed: ${res.debug}")
    yield res).tapError(e =>
      ZIO.logError(
        s"[PlaceSLOrder] Failed to place order for request: ${orderReq.debug}, reason: ${e.getMessage}"
      )
    )

  private def modifyOrder(orderReq: OrderRequest, order: Order) =
    (for
      _     <- ZIO.logDebug(s"Modifying order: ${order.debug}")
      quote <- kiteClient.getQuote(QuoteRequest.from(order))
      _     <- ZIO
                 .fail(LastPriceExceeds(quote.lastPrice, order.price))
                 .when(quote.lastPrice * 2.5 > order.price)

      (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
      modifyReq   = orderReq.toSLBuy(tp, price, order.tradingSymbol)
      res        <- kiteClient.modifyOrder(order.orderId, modifyReq, regular)
      _          <- ZIO.logDebug(s"Order modified for request: ${orderReq.debug}, response: $res")
    yield res)
      .tapError(e =>
        ZIO.logError(
          s"Order modification failed for request: ${orderReq.debug}, reason : ${e.getMessage}"
        )
      )

  private def closeOrder(orderReq: OrderRequest, order: Order) =
    for
      _   <- ZIO.logDebug(s"Closing order: ${order.debug}")
      res <- kiteClient.modifyOrder(order.orderId, orderReq, regular)
      _   <- ZIO.logDebug(s"Order closed: $res")
    yield res

  private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
    for
      _   <- ZIO.logDebug(s"Placing order: ${orderReq.debug}")
      res <- kiteService.placeOrder(orderReq, variety).tap(update)
      _   <- ZIO.logDebug(s"Order placed: ${res.debug}")
    yield res

object EverydayStrategyLive:
  val layer = ZLayer.fromFunction(EverydayStrategyLive.apply)
