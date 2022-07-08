package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.Instrument as ZInstrument
import dev.kpritam.zerodha.time.toIndiaLocalDate
import zio.json.*

import java.time.{LocalDate, ZoneId}
import java.util.Date

case class Instrument(
    instrumentToken: Long,
    exchangeToken: Long,
    tradingSymbol: String,
    name: String,
    lastPrice: Double,
    tickSize: Double,
    instrumentType: String,
    segment: String,
    exchange: String,
    strike: String,
    lotSize: Int,
    expiry: LocalDate
):
  def isCE: Boolean = instrumentType == "CE"
  def isPE: Boolean = instrumentType == "PE"

  def expiryDateEquals(that: LocalDate): Boolean = expiry.compareTo(that) == 0

object Instrument:
  given JsonCodec[Instrument] = DeriveJsonCodec.gen[Instrument]

  def from(instrument: ZInstrument): Instrument =
    Instrument(
      instrument.instrument_token,
      instrument.exchange_token,
      instrument.tradingsymbol,
      instrument.name,
      instrument.last_price,
      instrument.tick_size,
      instrument.instrument_type,
      instrument.segment,
      instrument.exchange,
      instrument.strike,
      instrument.lot_size,
      instrument.expiry.toIndiaLocalDate
    )

case class CEPEInstrument(ce: Instrument, pe: Instrument):
  val tokens: List[java.lang.Long] = List(ce.instrumentToken, pe.instrumentToken)

object CEPEInstrument:
  given JsonCodec[CEPEInstrument] = DeriveJsonCodec.gen[CEPEInstrument]
