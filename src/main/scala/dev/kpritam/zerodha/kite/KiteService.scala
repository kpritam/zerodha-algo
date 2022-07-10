package dev.kpritam.zerodha.kite

import java.util.Date
import dev.kpritam.zerodha.kite.models.*
import zio.*

trait KiteService:
  def getInstrumentsWithLTP(request: InstrumentRequest): Task[List[Instrument]]

  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument]

object KiteService:
  val live = ZLayer.fromFunction(KiteServiceLive.apply)

  def getInstrumentsWithLTP(request: InstrumentRequest): RIO[KiteService, List[Instrument]] =
    ZIO.serviceWithZIO(_.getInstrumentsWithLTP(request))

  def getCEPEInstrument(
      request: InstrumentRequest,
      price: Double
  ): RIO[KiteService, CEPEInstrument] =
    ZIO.serviceWithZIO(_.getCEPEInstrument(request, price))

case class KiteServiceLive(kiteClient: KiteClient) extends KiteService:
  def getInstrumentsWithLTP(request: InstrumentRequest): Task[List[Instrument]] =
    def token(i: Instrument) = QuoteRequest.InstrumentToken(i.instrumentToken)
    for
      instruments <- kiteClient.getInstruments(request)
      ltp         <- kiteClient.getLTPs(instruments.map(token))
    yield instruments.map(i => i.copy(lastPrice = ltp.getLastPriceOrZero(i.instrumentToken)))

  def getCEPEInstrument(request: InstrumentRequest, price: Double): Task[CEPEInstrument] =
    for
      instruments <- getInstrumentsWithLTP(request)
      ce          <- findInstrument(instruments.filter(_.isCE), price)
      pe          <- findInstrument(instruments.filter(_.isPE), ce.lastPrice)
    yield CEPEInstrument(ce, pe)

  private def findInstrument(instruments: List[Instrument], price: Double): Task[Instrument] =
    ZIO.getOrFail(instruments.minByOption(i => math.abs(i.lastPrice - price)))
