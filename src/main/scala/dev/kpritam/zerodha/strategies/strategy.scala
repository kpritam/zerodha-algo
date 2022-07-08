package dev.kpritam.zerodha.strategies

import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.models.QuoteRequest.InstrumentToken
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.kite.{KiteClient, KiteService, KiteTickerClient}
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*
import zio.json.*

import java.lang.Double
import java.util.Calendar

val nfo               = Exchange("NFO")
val nifty             = "NIFTY"
val expiryDay         = nextWeekday(Calendar.THURSDAY)
val instrumentRequest = InstrumentRequest(nfo, nifty, expiryDay)
val price             = 12
val regular           = "regular"

val orderRequest = OrderRequest(
  exchange = nfo.toString,
  validity = "Day",
  product = "NRML",
  orderType = "MARKET",
  transactionType = "SELL",
  quantity = 500
)

val strategy =
  for
    // login
    requestToken <- KiteLogin.login
    user         <- KiteLogin.createSession(requestToken)
    _            <- Console.printLine(s"${user.userName} logged in successfully.")

    cepe <- KiteService.getCEPEInstrument(instrumentRequest, price)
    _    <- Console.printLine(cepe.toJson)

    // place CE & PE market sell order
    ceOrder      <- Ref.make[Option[Order]](None)
    peOrder      <- Ref.make[Option[Order]](None)
    ceOrderFiber <- placeOrder(mkCEOrderRequest(orderRequest, cepe), regular, ceOrder).fork
    peOrderFiber <- placeOrder(mkPEOrderRequest(orderRequest, cepe), regular, peOrder).fork

    _ <- runOrderCompletionTasks(orderRequest, cepe.tokens, ceOrder, peOrder)

    _ <- ceOrderFiber.zip(peOrderFiber).join
  yield ()

private def mkCEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.ce.tradingSymbol, price = cepe.ce.strike.toDouble)

private def mkPEOrderRequest(orderReq: OrderRequest, cepe: CEPEInstrument) =
  orderReq.copy(tradingSymbol = cepe.pe.tradingSymbol, price = cepe.pe.strike.toDouble)

private def placeOrder(orderReq: OrderRequest, variety: String, ref: Ref[Option[Order]]) =
  KiteClient.placeOrder(orderReq, variety).tap(o => ref.set(Some(o)))

private def orderEq(o1: Option[Order], o2: Order) = o1.exists(_.orderId == o2.orderId)

private def placeSLBuyOrder(orderReq: OrderRequest, order: Order, ref: Ref[Option[Order]]) =
  for
    quote      <- KiteClient.getQuote(QuoteRequest.from(order))
    (tp, price) = triggerPriceAndPrice(order.averagePriceOrZero, quote)
    res        <- KiteClient.placeOrder(orderReq.toSLBuy(tp, price, order.tradingSymbol), regular)
    _          <- ref.set(Some(res))
  yield res

private def runOrderCompletionTasks(
    orderReq: OrderRequest,
    tokens: List[java.lang.Long],
    ceRef: Ref[Option[Order]],
    peRef: Ref[Option[Order]]
) =
  for
    ceSLOrderRef <- Ref.make[Option[Order]](None)
    peSLOrderRef <- Ref.make[Option[Order]](None)
    res          <- KiteTickerClient
                      .subscribeOrders(tokens)
                      .filter(_.status.equalsIgnoreCase("COMPLETE"))
                      .mapZIO { o =>
                        for
                          ceOrder   <- ceRef.get
                          peOrder   <- peRef.get
                          ceSLOrder <- ceSLOrderRef.get
                          peSLOrder <- peSLOrderRef.get
                          _         <- ZIO.when(orderEq(ceOrder, o))(placeSLBuyOrder(orderReq, o, ceSLOrderRef))
                          _         <- ZIO.when(orderEq(peOrder, o))(placeSLBuyOrder(orderReq, o, peSLOrderRef))
                          _         <- ZIO.when(orderEq(ceSLOrder, o))(modifyOrder(orderReq, peOrder))
                          _         <- ZIO.when(orderEq(peSLOrder, o))(modifyOrder(orderReq, ceOrder))
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
