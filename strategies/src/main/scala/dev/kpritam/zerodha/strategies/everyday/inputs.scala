package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.kite.models.InstrumentRequest
import dev.kpritam.zerodha.kite.models.OrderRequest
import dev.kpritam.zerodha.time.nextWeekday

import java.util.Calendar

private val priceNifty        = 12
private val priceBankNifty    = 25
private val regular           = "regular"
private val nfo               = Exchange("NFO")
private val nifty             = "NIFTY"
private val bankNifty         = "BANKNIFTY"
private val thursday          = Calendar.THURSDAY
private val quantityNifty     = 500
private val quantityBankNifty = 100
