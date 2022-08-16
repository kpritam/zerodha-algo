package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.catchAndLog
import dev.kpritam.zerodha.cron.*
import dev.kpritam.zerodha.kite.models.Exchange

import java.util.Calendar

val run =
  for
    f1 <- EverydayStrategy
            .sellBuyModifyOrder(nfo, nifty, thursday, quantity)
            .catchAndLog("Strategy failed")
            .schedule(onceDay(9, 25))
            .fork
    f2 <- EverydayStrategy.modifyPendingOrders
            .catchAndLog("[1:30] Modify failed")
            .schedule(onceDay(13, 30))
            .fork
    f3 <- EverydayStrategy.modifyPendingOrders
            .catchAndLog("[2:30] Modify failed")
            .schedule(onceDay(14, 30))
            .fork
    _  <- f1.zip(f2).zip(f3).await
  yield ()

val runTest =
  EverydayStrategy
    .sellBuyModifyOrder(nfo, nifty, thursday, quantity)
    .catchAndLog("Strategy failed")
