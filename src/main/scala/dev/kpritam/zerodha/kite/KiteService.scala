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

object KiteService:
  val live = ZLayer.fromFunction(KiteServiceLive.apply)

  def getInstrumentsWithLTP(
      exchange: Exchange,
      name: String,
      expiryDate: Date
  ): RIO[KiteService, List[Instrument]] =
    ZIO.serviceWithZIO(_.getInstrumentsWithLTP(exchange, name, expiryDate))

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
