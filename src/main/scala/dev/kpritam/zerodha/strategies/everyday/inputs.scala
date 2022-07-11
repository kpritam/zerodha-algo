package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.{Exchange, InstrumentRequest, OrderRequest}
import dev.kpritam.zerodha.time.nextWeekday

import java.util.Calendar

val nfo               = Exchange("NFO")
val nifty             = "NIFTY"
val expiryDay         = nextWeekday(Calendar.THURSDAY)
val instrumentRequest = InstrumentRequest(nfo, nifty, expiryDay)
val price             = 12
val regular           = "regular"

val orderRequest = OrderRequest(
  exchange = nfo.toString,
  validity = "DAY",
  product = "NRML",
  orderType = "MARKET",
  transactionType = "SELL",
  quantity = 50
)
