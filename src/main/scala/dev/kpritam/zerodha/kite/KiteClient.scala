package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.{LTPQuote, Order, OrderParams, Quote}
import dev.kpritam.zerodha.kite.models.*
import zio.*

import java.time.Instant
import java.util.Date
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala}

trait KiteClient:
  def getInstruments: Task[List[Instrument]]
  def getInstruments(exchange: Exchange): Task[List[Instrument]]
  def getInstruments(request: InstrumentRequest): Task[List[Instrument]]

  def getQuote(request: QuoteRequest): Task[Quote]
  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]]

  def getLTP(request: QuoteRequest): Task[LTPQuote]
  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]]

  def modifyOrder(orderId: String, orderParams: OrderParams, variety: String): Task[Order]
  def placeOrder(orderParams: OrderParams, variety: String): Task[Order]
  def getOrders: Task[List[Order]]
  def getOrders(orderIds: List[String]): Task[List[Order]]

case class KiteClientLive(kiteConnect: KiteConnect) extends KiteClient:
  def getInstruments: Task[List[Instrument]] =
    ZIO.attemptBlocking { kiteConnect.getInstruments.asScala.toList.map(Instrument.from) }

  def getInstruments(exchange: Exchange): Task[List[Instrument]] =
    ZIO.attemptBlocking {
      kiteConnect.getInstruments(exchange.toString).asScala.toList.map(Instrument.from)
    }

  def getInstruments(request: InstrumentRequest): Task[List[Instrument]] =
    getInstruments(request.exchange).map(
      _.filter(i => i.name == request.name && request.expiryDate.compareTo(i.expiry) == 0)
    )

  def getQuote(request: QuoteRequest): Task[Quote] =
    for
      quotes <- getQuotes(List(request))
      token  <- QuoteRequest.from(request.instrument)
      quote  <- ZIO.getOrFailWith(QuoteNoteFound(request.instrument))(quotes.get(token))
    yield quote

  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]] =
    for
      map    <- ZIO.attemptBlocking(kiteConnect.getQuote(request.map(_.instrument).toArray).asScala)
      quotes <- mkQuoteRequestMap(map.toMap)
    yield quotes

  def getLTP(request: QuoteRequest): Task[LTPQuote] =
    for
      ltpQuotes <- getLTPs(List(request))
      quote     <- QuoteRequest.from(request.instrument)
      ltpQuote  <- ZIO.getOrFailWith(QuoteNoteFound(request.instrument))(ltpQuotes.get(quote))
    yield ltpQuote

  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]] =
    for
      map       <- ZIO.attemptBlocking(kiteConnect.getLTP(request.map(_.instrument).toArray).asScala)
      ltpQuotes <- mkQuoteRequestMap(map.toMap)
    yield ltpQuotes

  def placeOrder(orderParams: OrderParams, variety: String): Task[Order] =
    ZIO.attemptBlocking {
      kiteConnect.placeOrder(orderParams, variety)
    }

  def modifyOrder(orderId: String, orderParams: OrderParams, variety: String): Task[Order] =
    ZIO.attemptBlocking {
      kiteConnect.modifyOrder(orderId, orderParams, variety)
    }

  def getOrders: Task[List[Order]] =
    ZIO.attemptBlocking { kiteConnect.getOrders.asScala.toList }

  def getOrders(orderIds: List[String]): Task[List[Order]] =
    getOrders.map(_.filter(order => orderIds.contains(order.orderId)))

  private def mkQuoteRequestMap[T](
      map: Map[String, T]
  ): IO[InvalidInstrumentToken, Map[QuoteRequest, T]] =
    ZIO.foreach(map)((k, v) => QuoteRequest.from(k).map(q => (q, v)))

object KiteClient:
  val live: ZLayer[KiteConnect, Nothing, KiteClientLive] =
    ZLayer.fromFunction(KiteClientLive.apply)

  def getInstruments: RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments)

  def getInstruments(exchange: Exchange): RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments(exchange))

  def getInstruments(request: InstrumentRequest): RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments(request))

  def getQuote(request: QuoteRequest): RIO[KiteClient, Quote] =
    ZIO.serviceWithZIO[KiteClient](_.getQuote(request))

  def getQuotes(request: List[QuoteRequest]): RIO[KiteClient, Map[QuoteRequest, Quote]] =
    ZIO.serviceWithZIO[KiteClient](_.getQuotes(request))

  def getLTP(request: QuoteRequest): RIO[KiteClient, LTPQuote] =
    ZIO.serviceWithZIO[KiteClient](_.getLTP(request))

  def getLTPs(request: List[QuoteRequest]): RIO[KiteClient, Map[QuoteRequest, LTPQuote]] =
    ZIO.serviceWithZIO[KiteClient](_.getLTPs(request))

  def placeOrder(orderParams: OrderParams, variety: String): RIO[KiteClient, Order] =
    ZIO.serviceWithZIO[KiteClient](_.placeOrder(orderParams, variety))

  def modifyOrder(
      orderId: String,
      orderParams: OrderParams,
      variety: String
  ): RIO[KiteClient, Order] =
    ZIO.serviceWithZIO[KiteClient](_.modifyOrder(orderId, orderParams, variety))

  def getOrders: RIO[KiteClient, List[Order]] = ZIO.serviceWithZIO[KiteClient](_.getOrders)

  def getOrders(orderIds: List[String]): RIO[KiteClient, List[Order]] =
    ZIO.serviceWithZIO[KiteClient](_.getOrders(orderIds))
