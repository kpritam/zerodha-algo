package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.Instrument as ZInstrument

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
    expiry: Date
) {
  def isCE: Boolean = instrumentType == "CE"
  def isPE: Boolean = instrumentType == "PE"
}

object Instrument:
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
      instrument.expiry
    )

case class CEPEInstrument(ce: Instrument, pe: Instrument)
