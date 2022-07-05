package dev.kpritam.zerodha.kite.models

import java.util.Date

case class InstrumentRequest(exchange: Exchange, name: String, expiryDate: Date)
