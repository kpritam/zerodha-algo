package dev.kpritam.zerodha.strategies.everyday

import com.zerodhatech.models.User
import dev.kpritam.zerodha.db.{Instruments, Orders}
import dev.kpritam.zerodha.kite.*
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.time.{indiaZone, nextWeekday}
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*

import java.time.LocalDateTime

trait EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): Task[Unit]

  def modifyPendingOrders(): Task[Unit]

object EverydayStrategy:
  val live = ZLayer.fromFunction(EverydayStrategyLive.apply)

  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): RIO[EverydayStrategy, Unit] =
    ZIO.serviceWithZIO[EverydayStrategy](_.sellBuyModifyOrder(exchange, name, expiryDay, quantity))

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
      _                <- seedInstrumentsIfNeeded(instrumentRequest)

      cepe <- kiteService.getCEPEInstrument(instrumentRequest, price)
      _    <- ZIO.logInfo(s"Selected instruments: ${cepe.toJson}")

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

  def modifyPendingOrders(): Task[Unit] = ???

  private def runOrderCompletionTasks(
      orderReq: OrderRequest,
      tokens: List[java.lang.Long],
      state: Ref[State]
  ) =
    for
      _   <- ZIO.logDebug(s"Subscribing to tokens: ${tokens.mkString(", ")}")
      res <- kiteService
               .subscribeOrders(tokens)
               .filter(_.completed)
               .mapZIO { o =>
                 for
                   _ <- ZIO.logDebug(s"[1] Order completed: $o")
                   _ <- ZIO.sleep(1.second)
                   s <- state.get.tap(s => ZIO.logDebug("[2] Current State: " + s))
                   _ <- ZIO.whenCase(Some(o.orderId)) {
                          case s.ceOrderId   => placeSLOrder(orderReq, o, state.updateCeSL)
                          case s.peOrderId   => placeSLOrder(orderReq, o, state.updatePeSL)
                          case s.ceSLOrderId => modifyOrder(orderReq, s.peOrder)
                          case s.peSLOrderId => modifyOrder(orderReq, s.ceOrder)
                        }
                 yield ()
               }
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

  private def modifyOrder(orderReq: OrderRequest, order: Option[Order]) =
    for
      _          <- ZIO.logDebug(s"Modifying order: $order")
      o          <- ZIO.getOrFail(order)
      quote      <- kiteClient.getQuote(QuoteRequest.from(o))
      _          <- ZIO.when(quote.lastPrice * 2.5 > o.price)(
                      ZIO.fail(LastPriceExceeds(quote.lastPrice, o.price))
                    )
      (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
      res        <-
        kiteClient.modifyOrder(o.orderId, orderReq.toSLBuy(tp, price, o.tradingSymbol), regular)
      _          <- ZIO.logDebug(s"Order modified: $res")
    yield res

  private def seedInstrumentsIfNeeded(request: InstrumentRequest) =
    for
      instruments <- instruments.all
      _           <- ZIO.unless(isAfter(instruments, 9, 15))(
                       ZIO.logDebug("Downloading instruments ...") *> kiteService.seedInstruments(request)
                     )
    yield ()

  private def isAfter(instruments: List[Instrument], hour: Int, min: Int) =
    instruments.exists { instrument =>
      val time = LocalDateTime.now(indiaZone).withHour(hour).withMinute(min)
      instrument.createdAt.isAfter(time)
    }

  private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
    for
      _   <- ZIO.logDebug(s"Placing order: $orderReq")
      res <- kiteService.placeOrder(orderReq, variety).tap(update)
      _   <- ZIO.logDebug(s"Order placed: $res")
    yield res
