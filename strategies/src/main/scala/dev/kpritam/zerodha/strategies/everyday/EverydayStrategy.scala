package dev.kpritam.zerodha.strategies.everyday

import com.zerodhatech.models.User
import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.kite.*
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.models.LastPriceExceeds
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*

trait EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): Task[Unit]

  def modifyPendingOrders: Task[List[String]]

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
      cepe             <- kiteService.getCEPEInstrument(instrumentRequest, i => math.abs(i.lastPrice - price))
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
          f1 <- runOrderCompletionTasks(orderReq, cepe.tokens, state).fork
          _  <- ZIO.sleep(1.seconds)

          // place CE & PE market sell order
          ceOrderReq = orderReq.copy(tradingSymbol = cepe.ce.tradingSymbol)
          peOrderReq = orderReq.copy(tradingSymbol = cepe.pe.tradingSymbol)
          f2        <- placeOrder(ceOrderReq, regular, state.updateCe).fork
          f3        <- placeOrder(peOrderReq, regular, state.updatePe).fork
          _         <- f2.zip(f3).zip(f1).join
        yield ()
    yield ()

  def modifyPendingOrders: Task[List[String]] =
    for
      pendingOrders <- orders.getPendingSL
      orders        <- kiteClient.getOrders(pendingOrders.map(_.orderId))
      res           <- ZIO.foreachPar(orders)(o => modifyOrder(OrderRequest.from(o), o))
    yield res

  private def runOrderCompletionTasks(
      orderReq: OrderRequest,
      tokens: List[java.lang.Long],
      state: Ref[State]
  ) =
    for
      _   <- ZIO.logDebug(s"Subscribing to tokens: ${tokens.mkString(", ")}")
      res <- kiteService
               .subscribeOrders(tokens)
               .collectZIO {
                 case o if o.completed =>
                   for
                     _ <- ZIO.logDebug(s"[1] Order completed: $o")
                     _ <- ZIO.sleep(1.second)
                     s <- state.get.tap(s => ZIO.logDebug("[2] Current State: " + s))
                     _ <- ZIO.whenCase(Some(o.orderId)) {
                            case s.ceOrderId => placeSLOrder(orderReq, o, state.updateCeSL)
                            case s.peOrderId => placeSLOrder(orderReq, o, state.updatePeSL)

                            case s.ceSLOrderId if s.peOrder.nonEmpty =>
                              modifyOrder(orderReq, s.peOrder.get)
                            case s.peSLOrderId if s.ceOrder.nonEmpty =>
                              modifyOrder(orderReq, s.ceOrder.get)
                          }
                   yield ()
               }
               .take(4)
               .runDrain
    yield res

  private def placeSLOrder(orderReq: OrderRequest, order: Order, update: Order => UIO[Unit]) =
    for
      _          <- ZIO.logDebug(s"Placing SL order: $orderReq")
      quote      <- kiteClient.getQuote(QuoteRequest.from(order))
      (tp, price) = triggerPriceAndPrice(order.averagePrice, quote)
      res        <-
        kiteService
          .placeOrder(orderReq.toSLBuy(tp, price, order.tradingSymbol), regular)
          .tap(update)
      _          <- ZIO.logDebug(s"SL order placed: $res")
    yield res

  private def modifyOrder(orderReq: OrderRequest, order: Order) =
    for
      _          <- ZIO.logDebug(s"Modifying order: $order")
      quote      <- kiteClient.getQuote(QuoteRequest.from(order))
      _          <- ZIO
                      .fail(LastPriceExceeds(quote.lastPrice, order.price))
                      .when(quote.lastPrice * 2.5 > order.price)
      (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
      res        <-
        kiteClient.modifyOrder(
          order.orderId,
          orderReq.toSLBuy(tp, price, order.tradingSymbol),
          regular
        )
      _          <- ZIO.logDebug(s"Order modified: $res")
    yield res

  private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
    for
      _   <- ZIO.logDebug(s"Placing order: $orderReq")
      res <- kiteService.placeOrder(orderReq, variety).tap(update)
      _   <- ZIO.logDebug(s"Order placed: $res")
    yield res
