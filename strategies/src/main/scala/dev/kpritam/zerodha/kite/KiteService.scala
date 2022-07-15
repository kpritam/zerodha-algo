package dev.kpritam.zerodha.kite

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.getLastPriceOrZero
import dev.kpritam.zerodha.kite.models.*
import zio.*
import zio.stream.UStream

import java.lang
import java.sql.SQLException
import java.util.Date
import javax.sql.DataSource

trait KiteService:
  def getInstrumentsWithLTP(request: InstrumentRequest): Task[List[Instrument]]
  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument]
  def seedInstruments(request: InstrumentRequest): Task[List[Long]]
  def placeOrder(request: OrderRequest, variety: String): Task[Order]

  def subscribeOrders(tokens: List[lang.Long]): UStream[Order]

object KiteService:
  val live = ZLayer.fromFunction(KiteServiceLive.apply)

  def getInstrumentsWithLTP(request: InstrumentRequest): RIO[KiteService, List[Instrument]] =
    ZIO.serviceWithZIO(_.getInstrumentsWithLTP(request))

  def getCEPEInstrument(
      request: InstrumentRequest,
      price: Double
  ): RIO[KiteService, CEPEInstrument] =
    ZIO.serviceWithZIO(_.getCEPEInstrument(request, price))

  def seedInstruments(request: InstrumentRequest): RIO[KiteService, List[Long]] =
    ZIO.serviceWithZIO(_.seedInstruments(request))

  def placeOrder(request: OrderRequest, variety: String): RIO[KiteService, Order] =
    ZIO.serviceWithZIO[KiteService](_.placeOrder(request, variety))

case class KiteServiceLive(
    kiteClient: KiteClient,
    kiteTickerClient: KiteTickerClient,
    instruments: Instruments,
    orders: Orders
) extends KiteService:
  def getInstrumentsWithLTP(request: InstrumentRequest): Task[List[Instrument]] =
    def token(i: Instrument) = QuoteRequest.InstrumentToken(i.instrumentToken)
    for
      instruments <- kiteClient.getInstruments(request)
      ltp         <- kiteClient.getLTPs(instruments.map(token))
    yield instruments.map(i => i.copy(lastPrice = ltp.getLastPriceOrZero(i.instrumentToken)))

  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument] =
    for
      instruments <- instruments.all
      ce          <- findInstrument(instruments.filter(_.isCE), price)
      pe          <- findInstrument(instruments.filter(_.isPE), ce.lastPrice)
    yield CEPEInstrument(ce, pe)

  def seedInstruments(request: InstrumentRequest): Task[List[Long]] =
    getInstrumentsWithLTP(request).flatMap(instruments.seed)

  def placeOrder(request: OrderRequest, variety: String): Task[Order] =
    for
      orderId <- kiteClient.placeOrder(request, variety)
      order   <- kiteClient.getOrder(orderId)
      _       <- orders.create(order)
    yield order

  private def findInstrument(instruments: List[Instrument], price: Double): Task[Instrument] =
    ZIO.getOrFail(instruments.minByOption(i => math.abs(i.lastPrice - price)))

  def subscribeOrders(tokens: List[lang.Long]): UStream[Order] =
    kiteTickerClient
      .subscribe(tokens)
      .tap(o =>
        orders
          .update(o)
          .catchAll(e =>
            ZIO.logErrorCause(s"Failed to update OrderId: ${o.orderId}", Cause.fail(e))
          )
      )
