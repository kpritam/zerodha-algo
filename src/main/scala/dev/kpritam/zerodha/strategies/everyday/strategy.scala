package dev.kpritam.zerodha.strategies.everyday

import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.models.QuoteRequest.InstrumentToken
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.kite.{KiteClient, KiteService, KiteTickerClient, KiteTickerLive}
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*

import java.lang.Double
import java.util.Calendar

val strategy =
  for
    // state
    state <- State.make

    // login
    requestToken <- KiteLogin.login
    user         <- KiteLogin.createSession(requestToken)
    _            <- Console.printLine(s"${user.userName} logged in successfully.")

    cepe <- KiteService.getCEPEInstrument(instrumentRequest, price)
    _    <- Console.printLine(cepe.toJson)

    // place CE & PE market sell order
    ceOrderFiber <- placeOrder(mkCEOrderRequest(orderRequest, cepe), regular, state.updateCe).fork
    peOrderFiber <- placeOrder(mkPEOrderRequest(orderRequest, cepe), regular, state.updatePe).fork

    _ <- runOrderCompletionTasks(orderRequest, cepe.tokens, state)
           .provideSomeLayer(
             ZLayer.succeed(KiteTickerLive(KiteTicker(user.accessToken, user.apiKey)))
           )

    _ <- ceOrderFiber.zip(peOrderFiber).join
  yield ()

private def mkCEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.ce.tradingSymbol, price = cepe.ce.strike)

private def mkPEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.pe.tradingSymbol, price = cepe.pe.strike)

private def placeOrder(orderReq: OrderRequest, variety: String, update: Order => UIO[Unit]) =
  KiteClient.placeOrder(orderReq, variety).tap(update)

private def orderEq(o1: Option[Order], o2: Order) = o1.exists(_.orderId == o2.orderId)

private def placeSLOrder(orderReq: OrderRequest, order: Order, update: Order => UIO[Unit]) =
  for
    quote      <- KiteClient.getQuote(QuoteRequest.from(order))
    (tp, price) = triggerPriceAndPrice(order.avgPriceOrZero, quote)
    res        <- KiteClient.placeOrder(orderReq.toSLBuy(tp, price, order.tradingSymbol), regular)
    _          <- update(res)
  yield res

private def runOrderCompletionTasks(
    orderReq: OrderRequest,
    tokens: List[java.lang.Long],
    state: Ref[State]
) =
  for res <- KiteTickerClient
               .subscribeOrders(tokens)
               .filter(_.completed)
               .mapZIO { o =>
                 for
                   s <- state.get
                   _ <- ZIO.when(orderEq(s.ceOrder, o))(placeSLOrder(orderReq, o, state.updateCeSL))
                   _ <- ZIO.when(orderEq(s.peOrder, o))(placeSLOrder(orderReq, o, state.updatePeSL))
                   _ <- ZIO.when(orderEq(s.ceSLOrder, o))(modifyOrder(orderReq, s.peOrder))
                   _ <- ZIO.when(orderEq(s.peSLOrder, o))(modifyOrder(orderReq, s.ceOrder))
                 yield ()
               }
               .runDrain
  yield res

private def modifyOrder(orderReq: OrderRequest, order: Option[Order]) =
  for
    o          <- ZIO.getOrFail(order)
    newOrder    = orderReq.copy(exchange = o.exchange)
    quote      <- KiteClient.getQuote(QuoteRequest.from(o))
    _          <- ZIO.when(quote.lastPrice * 2.5 > o.priceOrZero)(
                    ZIO.fail(LastPriceExceeds(quote.lastPrice, o.priceOrZero))
                  )
    (tp, price) = triggerPriceAndPrice(quote.lastPrice, quote)
    res        <- KiteClient.modifyOrder(o.orderId, newOrder.toSLBuy(tp, price, o.tradingSymbol), regular)
  yield res
