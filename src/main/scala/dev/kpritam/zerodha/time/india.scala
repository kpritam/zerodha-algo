package dev.kpritam.zerodha.time

import java.time.ZoneId
import java.util.Date

val indiaZone = ZoneId.of("Asia/Kolkata")

extension (date: Date) def toIndiaLocalDate = date.toInstant.atZone(indiaZone).toLocalDate
