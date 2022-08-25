package dev.kpritam.zerodha.kite

import dev.kpritam.zerodha.kite.models.*
import zio.*
import zio.stream.UStream

import java.lang
import java.time.LocalDate

trait KiteService:
  def seedAllInstruments(expiryDate: LocalDate): Task[List[Long]]
  def getCEPEInstrument[A: Ordering](
      request: InstrumentRequest,
      minBy: Instrument => A
  ): Task[CEPEInstrument]
  def placeOrder(request: OrderRequest, variety: String): Task[Order]

  def subscribeOrders(tokens: List[lang.Long]): UStream[Order]

object KiteService:

  def seedAllInstruments(expiryDate: LocalDate): RIO[KiteService, List[Long]] =
    ZIO.serviceWithZIO(_.seedAllInstruments(expiryDate))
