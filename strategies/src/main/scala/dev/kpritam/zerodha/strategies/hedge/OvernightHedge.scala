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

// 3:25 => Get Nifty price LTP
// find closest PE instrument (%50)
// place PE order
