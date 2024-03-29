package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.cron.*
import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.utils.catchAndLog

import java.util.Calendar

val run =
  for
    f1 <- EverydayStrategy
            .sellBuyModifyOrder(nfo, nifty, priceNifty, thursday, quantityNifty)
            .catchAndLog("[NIFTY] Strategy failed")
            .zipPar(
              EverydayStrategy
                .sellBuyModifyOrder(nfo, bankNifty, priceBankNifty, thursday, quantityBankNifty)
                .catchAndLog("[BANK NIFTY] Strategy failed")
            )
            .schedule(onceDay(9, 25))
            .fork
    f2 <- EverydayStrategy.modifyPendingOrders
            .catchAndLog("[1:30] Modify pending orders failed")
            .schedule(onceDay(13, 30))
            .fork
    f3 <- EverydayStrategy.modifyPendingOrders
            .catchAndLog("[2:30] Modifying pending orders failed")
            .schedule(onceDay(14, 30))
            .fork
    f4 <- EverydayStrategy.closePendingOrders
            .catchAndLog("[3:24] Closing pending orders failed")
            .schedule(onceDay(15, 24))
            .fork
    _  <- f1.zip(f2).zip(f3).zip(f4).await
  yield ()
