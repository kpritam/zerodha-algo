package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.LTPQuote
import com.zerodhatech.models.Quote
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.models.*
import zio.*

import java.time.Instant
import java.util.Date
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.MapHasAsScala

trait KiteClient:
  def getInstruments: Task[List[Instrument]]
  def getInstruments(exchange: Exchange): Task[List[Instrument]]
  def getInstruments(request: InstrumentRequest): Task[List[Instrument]]

  def getQuote(request: QuoteRequest): Task[Quote]
  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]]

  def getLTP(request: QuoteRequest): Task[LTPQuote]
  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]]

  def modifyOrder(orderId: String, orderReq: OrderRequest, variety: String): Task[String]
  def placeOrder(orderReq: OrderRequest, variety: String): Task[String]
  def getOrders: Task[List[Order]]
  def getOrders(orderIds: List[String]): Task[List[Order]]
  def getOrder(orderId: String): Task[Order]

case class KiteClientLive(kiteConnect: KiteConnect, login: KiteLogin) extends KiteClient:
  def getInstruments: Task[List[Instrument]] =
    attemptBlockingWithRetry(kiteConnect.getInstruments.asScala.toList.map(Instrument.from))

  def getInstruments(exchange: Exchange): Task[List[Instrument]] =
    attemptBlockingWithRetry {
      kiteConnect.getInstruments(exchange.toString).asScala.toList.map(Instrument.from)
    }

  def getInstruments(request: InstrumentRequest): Task[List[Instrument]] =
    getInstruments(request.exchange).map(
      _.filter(i => i.name == request.name && i.expiryDateEquals(request.expiryDate))
    )

  def getQuote(request: QuoteRequest): Task[Quote] =
    for
      quotes <- getQuotes(List(request))
      token  <- QuoteRequest.from(request.instrument)
      quote  <- ZIO.getOrFailWith(KiteError.QuoteNoteFound(request.instrument))(quotes.get(token))
    yield quote

  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]] =
    for
      map    <- attemptBlockingWithRetry(
                  kiteConnect.getQuote(request.map(_.instrument).toArray).asScala
                )
      quotes <- mkQuoteRequestMap(map.toMap)
    yield quotes

  def getLTP(request: QuoteRequest): Task[LTPQuote] =
    for
      ltpQuotes <- getLTPs(List(request))
      quote     <- QuoteRequest.from(request.instrument)
      ltpQuote  <-
        ZIO.getOrFailWith(KiteError.QuoteNoteFound(request.instrument))(ltpQuotes.get(quote))
    yield ltpQuote

  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]] =
    for
      map       <- attemptBlockingWithRetry(kiteConnect.getLTP(request.map(_.instrument).toArray).asScala)
      ltpQuotes <- mkQuoteRequestMap(map.toMap)
    yield ltpQuotes

  def placeOrder(orderReq: OrderRequest, variety: String): Task[String] =
    attemptBlockingWithRetry(kiteConnect.placeOrder(orderReq.toZerodha, variety).orderId)

  def modifyOrder(orderId: String, orderReq: OrderRequest, variety: String): Task[String] =
    attemptBlockingWithRetry(kiteConnect.modifyOrder(orderId, orderReq.toZerodha, variety).orderId)

  def getOrders: Task[List[Order]] =
    attemptBlockingWithRetry(kiteConnect.getOrders.asScala.toList.map(_.toOrder))

  def getOrder(orderId: String): Task[Order] =
    attemptBlockingWithRetry {
      kiteConnect.getOrderHistory(orderId).asScala.toList.maxBy(_.orderTimestamp).toOrder
    }

  def getOrders(orderIds: List[String]): Task[List[Order]] =
    getOrders.map(_.filter(order => orderIds.contains(order.orderId)))

  private def mkQuoteRequestMap[T](
      map: Map[String, T]
  ): IO[KiteError.InvalidInstrumentToken, Map[QuoteRequest, T]] =
    ZIO.foreach(map)((k, v) => QuoteRequest.from(k).map(q => (q, v)))

  private def attemptBlocking[A](effect: => A): IO[KiteError, A] =
    ZIO.attemptBlocking(effect).mapError(KiteError(_))

  private def attemptBlockingWithRetry[A](effect: => A): Task[A] =
    attemptBlocking(effect).catchSome { case KiteError.SessionExpired(msg, _) =>
      for
        _   <- ZIO.logWarning(s"Session expired. Logging in again. Error: $msg")
        _   <- login.login.map(user => kiteConnect.setAccessToken(user.accessToken))
        _   <- ZIO.logInfo("Login successful, retrying request ...")
        res <- attemptBlocking(effect)
      yield res
    }

object KiteClient:
  val live: ZLayer[KiteConnect & KiteLogin, Nothing, KiteClientLive] =
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

  def placeOrder(orderReq: OrderRequest, variety: String): RIO[KiteClient, String] =
    ZIO.serviceWithZIO[KiteClient](_.placeOrder(orderReq, variety))

  def modifyOrder(
      orderId: String,
      orderReq: OrderRequest,
      variety: String
  ): RIO[KiteClient, String] =
    ZIO.serviceWithZIO[KiteClient](_.modifyOrder(orderId, orderReq, variety))

  def getOrders: RIO[KiteClient, List[Order]] = ZIO.serviceWithZIO[KiteClient](_.getOrders)

  def getOrders(orderIds: List[String]): RIO[KiteClient, List[Order]] =
    ZIO.serviceWithZIO[KiteClient](_.getOrders(orderIds))

  def getOrder(orderId: String): RIO[KiteClient, Order] =
    ZIO.serviceWithZIO[KiteClient](_.getOrder(orderId))
