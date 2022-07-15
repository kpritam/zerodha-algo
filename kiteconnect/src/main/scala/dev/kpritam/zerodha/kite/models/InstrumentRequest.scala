package dev.kpritam.zerodha.kite.models

import java.time.LocalDate
import java.util.Date

case class InstrumentRequest(exchange: Exchange, name: String, expiryDate: LocalDate)
