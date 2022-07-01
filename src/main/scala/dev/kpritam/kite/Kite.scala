package dev.kpritam.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.{Instrument, LTPQuote, Quote}
import dev.kpritam.kite.models.*
import zio.*

import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala}
import scala.util.Try

trait Kite:
  def getInstruments: Task[List[Instrument]]
  def getInstruments(exchange: Exchange): Task[List[Instrument]]

  def getQuote(request: QuoteRequest): Task[Quote]
  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]]

  def getLTP(request: QuoteRequest): Task[LTPQuote]
  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]]

case class KiteLive(kiteConnect: KiteConnect) extends Kite:
  def getInstruments: Task[List[Instrument]] =
    ZIO.attemptBlocking { kiteConnect.getInstruments.asScala.toList }

  def getInstruments(exchange: Exchange): Task[List[Instrument]] =
    ZIO.attemptBlocking { kiteConnect.getInstruments(exchange.toString).asScala.toList }

  def getQuote(request: QuoteRequest): Task[Quote] =
    for {
      quotes <- getQuotes(List(request))
      quote <-
        ZIO
          .fromOption(quotes.get(QuoteRequest.from(request.instrument)))
          .orElseFail(QuoteNoteFound(request.instrument))
    } yield quote

  def getQuotes(request: List[QuoteRequest]): Task[Map[QuoteRequest, Quote]] =
    ZIO.attemptBlocking {
      kiteConnect
        .getQuote(request.map(_.instrument).toArray)
        .asScala
        .map((k, v) => (QuoteRequest.from(k), v))
        .toMap
    }

  def getLTP(request: QuoteRequest): Task[LTPQuote] =
    for {
      ltpQuotes <- getLTPs(List(request))
      ltpQuote <- ZIO.fromEither(
        ltpQuotes
          .get(QuoteRequest.from(request.instrument))
          .toRight(QuoteNoteFound(request.instrument))
      )
    } yield ltpQuote

  def getLTPs(request: List[QuoteRequest]): Task[Map[QuoteRequest, LTPQuote]] =
    ZIO.attemptBlocking {
      kiteConnect
        .getLTP(request.map(_.instrument).toArray)
        .asScala
        .map((k, v) => (QuoteRequest.from(k), v))
        .toMap
    }

object Kite:
  val live: ZLayer[KiteConnect, Nothing, KiteLive] =
    ZLayer { ZIO.service[KiteConnect].map(KiteLive.apply) }

  def getInstruments: ZIO[Kite, Throwable, List[Instrument]] =
    ZIO.serviceWithZIO[Kite](_.getInstruments)
  def getInstruments(exchange: Exchange): ZIO[Kite, Throwable, List[Instrument]] =
    ZIO.serviceWithZIO[Kite](_.getInstruments(exchange))
  def getQuote(request: QuoteRequest): ZIO[Kite, Throwable, Quote] =
    ZIO.serviceWithZIO[Kite](_.getQuote(request))
  def getQuotes(request: List[QuoteRequest]): ZIO[Kite, Throwable, Map[QuoteRequest, Quote]] =
    ZIO.serviceWithZIO[Kite](_.getQuotes(request))
  def getLTP(request: QuoteRequest): ZIO[Kite, Throwable, LTPQuote] =
    ZIO.serviceWithZIO[Kite](_.getLTP(request))
  def getLTPs(request: List[QuoteRequest]): ZIO[Kite, Throwable, Map[QuoteRequest, LTPQuote]] =
    ZIO.serviceWithZIO[Kite](_.getLTPs(request))
