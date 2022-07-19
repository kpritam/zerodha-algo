package dev.kpritam.zerodha.strategies.hedge

import dev.kpritam.zerodha.kite.models.{Exchange, InstrumentRequest, OrderRequest, TradingSymbol}
import dev.kpritam.zerodha.time.nextWeekday

import java.util.Calendar

private val nse        = Exchange("NSE")
private val nfo        = Exchange("NFO")
private val nifty50    = TradingSymbol("NIFTY 50")
private val nifty      = "NIFTY"
private val expiryDate = nextWeekday(Calendar.THURSDAY)
private val quantity   = 50
private val regular    = "regular"

private val instrumentReq = InstrumentRequest(nfo, nifty, expiryDate)

private def marketBuyOrder(tradingSymbol: String) =
  OrderRequest(
    exchange = nfo.toString,
    validity = "DAY",
    product = "NRML",
    orderType = "MARKET",
    transactionType = "BUY",
    quantity = quantity,
    tradingSymbol = tradingSymbol
  )
