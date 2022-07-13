package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.models.QuoteRequest.InstrumentToken
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.kite.{KiteClient, KiteService, KiteTickerClient, KiteTickerLive}
import dev.kpritam.zerodha.strategies.seedInstrumentsIfNeeded
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*
import zio.logging.*

import java.lang.Double
import scala.concurrent.duration.DurationInt

val everyday =
  for
    // state
    state <- State.make

    // login
    requestToken <- KiteLogin.login
    user         <- KiteLogin.createSession(requestToken)
    _            <- ZIO.logInfo(s"${user.userName} logged in successfully.")

    // seed
    _ <- seedInstrumentsIfNeeded(instrumentRequest)

    cepe <- KiteService.getCEPEInstrument(instrumentRequest, price)
    _    <- ZIO.logInfo(s"Selected instruments: ${cepe.toJson}")

    _ <-
      (for
        _  <- KiteTickerClient.init
        f1 <- runOrderCompletionTasks(orderRequest, cepe.tokens, state).fork
        _  <- ZIO.sleep(1.seconds)

        // place CE & PE market sell order
        f2 <- placeOrder(mkCEOrderRequest(orderRequest, cepe), regular, state.updateCe).fork
        f3 <- placeOrder(mkPEOrderRequest(orderRequest, cepe), regular, state.updatePe).fork
        _  <- f2.zip(f3).zip(f1).join
      yield ()).provideSomeLayer(KiteTickerClient.live(user))
  yield ()

private def mkCEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.ce.tradingSymbol)

private def mkPEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.pe.tradingSymbol)

private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
  for
    _   <- ZIO.logDebug(s"Placing order: $orderReq")
    res <- KiteService.placeOrder(orderReq, variety).tap(update)
    _   <- ZIO.logDebug(s"Order placed: $res")
  yield res

private def placeSLOrder(orderReq: OrderRequest, order: Order, update: Order => UIO[Unit]) =
  for
    _          <- ZIO.logDebug(s"Placing SL order: $orderReq")
    quote      <- KiteClient.getQuote(QuoteRequest.from(order))
    (tp, price) = triggerPriceAndPrice(order.averagePrice, quote)
    res        <-
      KiteService.placeOrder(orderReq.toSLBuy(tp, price, order.tradingSymbol), regular).tap(update)
    _          <- ZIO.logDebug(s"SL order placed: $res")
  yield res

private def runOrderCompletionTasks(
    orderReq: OrderRequest,
    tokens: List[java.lang.Long],
    state: Ref[State]
) =
  for
    _   <- ZIO.logDebug(s"Subscribing to tokens: ${tokens.mkString(", ")}")
    res <- KiteTickerClient
             .subscribeOrders(tokens)
             .filter(_.completed)
             .mapZIO { o =>
               for
                 _ <- ZIO.logDebug(s"[1] Order completed: $o")
                 _ <- state.get.tap(s => ZIO.logDebug("[2] Current State: " + s))
                 _ <- ZIO.sleep(2.seconds)
                 s <- state.get.tap(s => ZIO.logDebug("[3] Current State: " + s))
                 _ <- ZIO.logDebug(s"[4] OrderId: ${o.orderId}")
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

private def modifyOrder(orderReq: OrderRequest, order: Option[Order]) =
  for
    _          <- ZIO.logDebug(s"Modifying order: $order")
    o          <- ZIO.getOrFail(order)
    quote      <- KiteClient.getQuote(QuoteRequest.from(o))
    _          <- ZIO.when(quote.lastPrice * 2.5 > o.price)(
                    ZIO.fail(LastPriceExceeds(quote.lastPrice, o.price))
                  )
    (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
    res        <- KiteClient.modifyOrder(o.orderId, orderReq.toSLBuy(tp, price, o.tradingSymbol), regular)
    _          <- ZIO.logDebug(s"Order modified: $res")
  yield res
