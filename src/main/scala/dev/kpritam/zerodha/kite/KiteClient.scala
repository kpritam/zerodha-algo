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
  def getInstruments(exchange: Exchange, name: String, expiryDate: Date): Task[List[Instrument]]

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

  def getInstruments(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): Task[List[Instrument]] =
    getInstruments(exchange).map(
      _.filter(i => i.name == name && expiryDate.compareTo(i.expiry) == 0)
    )

  def getQuote(request: QuoteRequest): Task[Quote] =
    for
      quotes <- getQuotes(List(request))
      quote  <-
        ZIO
          .fromOption(quotes.get(QuoteRequest.from(request.instrument)))
          .orElseFail(QuoteNoteFound(request.instrument))
    yield quote

  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]] =
    ZIO.attemptBlocking {
      kiteConnect
        .getQuote(request.map(_.instrument).toArray)
        .asScala
        .map((k, v) => (QuoteRequest.from(k), v))
        .toMap
    }

  def getLTP(request: QuoteRequest): Task[LTPQuote] =
    for
      ltpQuotes <- getLTPs(List(request))
      ltpQuote  <- ZIO.fromEither(
                     ltpQuotes
                       .get(QuoteRequest.from(request.instrument))
                       .toRight(QuoteNoteFound(request.instrument))
                   )
    yield ltpQuote

  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]] =
    ZIO.attemptBlocking {
      kiteConnect
        .getLTP(request.map(_.instrument).toArray)
        .asScala
        .map((k, v) => (QuoteRequest.from(k), v))
        .toMap
    }

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

object KiteClient:
  val live: ZLayer[KiteConnect, Nothing, KiteClientLive] =
    ZLayer.fromFunction(KiteClientLive.apply)

  def getInstruments: RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments)

  def getInstruments(exchange: Exchange): RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments(exchange))

  def getInstruments(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): RIO[KiteClient, List[Instrument]] =
    ZIO.serviceWithZIO[KiteClient](_.getInstruments(exchange, name, expiryDate))

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
