package dev.kpritam.zerodha.kite

import java.util.Date
import dev.kpritam.zerodha.kite.models.*
import zio.*

trait KiteService:
  def getInstrumentsWithLTP(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): Task[List[Instrument]]

  def getCEPEInstrument(
      exchange: Exchange,
      name: String,
      expiryDate: Date,
      price: Double
  ): Task[CEPEInstrument]

object KiteService:
  val live = ZLayer.fromFunction(KiteServiceLive.apply)

  def getInstrumentsWithLTP(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): RIO[KiteService, List[Instrument]] =
    ZIO.serviceWithZIO(_.getInstrumentsWithLTP(exchange, name, expiryDate))

  def getCEPEInstrument(
      exchange: Exchange,
      name: String,
      expiryDate: Date,
      price: Double
  ): RIO[KiteService, CEPEInstrument] =
    ZIO.serviceWithZIO(_.getCEPEInstrument(exchange, name, expiryDate, price))

case class KiteServiceLive(kiteClient: KiteClient) extends KiteService:
  def getInstrumentsWithLTP(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): Task[List[Instrument]] =
    def token(i: Instrument) = QuoteRequest.InstrumentToken(i.instrumentToken)
    for
      instruments <- kiteClient.getInstruments(exchange, name, expiryDate)
      ltp         <- kiteClient.getLTPs(instruments.map(token))
    yield instruments.map(i => i.copy(lastPrice = ltp.get(token(i)).map(_.lastPrice).getOrElse(0)))

  def getCEPEInstrument(
      exchange: Exchange,
      name: String,
      expiryDate: Date,
      price: Double
  ): Task[CEPEInstrument] =
    for
      instruments <- getInstrumentsWithLTP(exchange, name, expiryDate)
      ce          <- findInstrument(instruments.filter(_.isCE), price)
      pe          <- findInstrument(instruments.filter(_.isPE), ce.lastPrice)
    yield CEPEInstrument(ce, pe)

  private def findInstrument(instruments: List[Instrument], price: Double): Task[Instrument] =
    ZIO.getOrFail(instruments.minByOption(i => math.abs(i.lastPrice - price)))
