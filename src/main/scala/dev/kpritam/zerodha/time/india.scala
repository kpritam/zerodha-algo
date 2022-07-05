package dev.kpritam.zerodha.time

import java.time.ZoneId
import java.util.Date

extension (date: Date)
  def toIndiaLocalDate = date.toInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate
