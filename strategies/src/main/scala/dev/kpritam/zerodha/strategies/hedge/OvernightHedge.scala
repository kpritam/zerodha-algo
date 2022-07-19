package dev.kpritam.zerodha.strategies.hedge

import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.KiteClient
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.models.*
import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.time.nextWeekday
import dev.kpritam.zerodha.utils.triggerPriceAndPrice
import zio.*

import java.time.LocalDateTime
import java.util.Calendar

// 3:25 => Get Nifty price LTP
// find closest PE instrument (%50)
// place PE order

trait OvernightHedge:
  def placePEOrder: Task[Order]

object OvernightHedge:
  val live = ZLayer.fromFunction(OvernightHedgeLive.apply)

  def placePEOrder: RIO[OvernightHedge, Order] =
    ZIO.serviceWithZIO[OvernightHedge](_.placePEOrder)

case class OvernightHedgeLive(kiteClient: KiteClient, kiteService: KiteService)
    extends OvernightHedge:
  override def placePEOrder: Task[Order] =
    for
      ltp  <- kiteClient.getLTP(QuoteRequest.Instrument(nifty50, nse))
      cepe <- kiteService.getCEPEInstrument(instrumentReq, i => math.abs(i.strike - ltp.lastPrice))
      res  <- kiteService.placeOrder(marketBuyOrder(cepe.pe.tradingSymbol), regular)
    yield res
