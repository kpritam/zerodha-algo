package dev.kpritam.zerodha.kite

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.getLastPriceOrZero
import dev.kpritam.zerodha.kite.models.*
import zio.*
import zio.stream.UStream

import java.lang
import java.sql.SQLException
import java.time.LocalDate
import java.util.Date
import javax.sql.DataSource

trait KiteService:
  def seedAllInstruments(expiryDate: LocalDate): Task[List[Long]]
  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument]
  def placeOrder(request: OrderRequest, variety: String): Task[Order]

  def subscribeOrders(tokens: List[lang.Long]): UStream[Order]

object KiteService:
  val live = ZLayer.fromFunction(KiteServiceLive.apply)

  def seedAllInstruments(expiryDate: LocalDate): RIO[KiteService, List[Long]] =
    ZIO.serviceWithZIO(_.seedAllInstruments(expiryDate))

case class KiteServiceLive(
    kiteClient: KiteClient,
    kiteTickerClient: KiteTickerClient,
    instruments: Instruments,
    orders: Orders
) extends KiteService:
  def seedAllInstruments(expiryDate: LocalDate): Task[List[Long]] =
    kiteClient.getInstruments
      .flatMap(i => instruments.seed(i.filter(_.expiryDateEquals(expiryDate))))

  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument] =
    for
      i       <- instruments.all.map(_.filter(_.eq(request)))
      updated <- getLTPs(i)
      ce      <- findInstrument(updated.filter(_.isCE), price)
      pe      <- findInstrument(updated.filter(_.isPE), ce.lastPrice)
    yield CEPEInstrument(ce, pe)

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
            ZIO.logError(s"Failed to update OrderId: ${o.orderId}, reason: ${e.getMessage}")
          )
      )

  private def getLTPs(instruments: List[Instrument]): Task[List[Instrument]] =
    kiteClient
      .getLTPs(instruments.map(i => QuoteRequest.InstrumentToken(i.instrumentToken)))
      .map(ltp =>
        instruments.map(i => i.copy(lastPrice = ltp.getLastPriceOrZero(i.instrumentToken)))
      )
