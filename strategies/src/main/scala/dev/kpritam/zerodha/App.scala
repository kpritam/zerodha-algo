package dev.kpritam.zerodha

import java.util.Calendar

import dev.kpritam.zerodha.Layers
import dev.kpritam.zerodha.db.{Instruments, Orders}
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig, KiteService, KiteTickerClient}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.strategies.everyday.EverydayStrategy
import dev.kpritam.zerodha.cron.*

import zio.*

object App extends ZIOAppDefault:

  def run: ZIO[Any, Any, Any] =
    (for
      f1 <- sellBuyModifyOrder.catchAndLog("Strategy failed")
      f2 <- EverydayStrategy.modifyPendingOrders
              .catchAndLog("[1:30] Modify failed")
              .schedule(everyNoon1_30)
              .fork
      f3 <- EverydayStrategy.modifyPendingOrders
              .catchAndLog("[2:30] Modify failed")
              .schedule(everyNoon2_30)
              .fork
      _  <- f2.zip(f3).await
    yield ())
      .provide(
        logging.console(logLevel = LogLevel.All),
        Totp.live,
        Layers.sttpBackend,
        // kite
        Layers.kiteConnectLive,
        Layers.kiteTickerLive,
        KiteConfig.live,
        KiteLogin.live,
        KiteClient.live,
        KiteTickerClient.live,
        // service
        KiteService.live,
        // db
        Instruments.live,
        Orders.live,

        // strategies
        EverydayStrategy.live
      )

  private def sellBuyModifyOrder =
    KiteTickerClient.init *> EverydayStrategy
      .sellBuyModifyOrder(
        exchange = Exchange("NFO"),
        name = "NIFTY",
        expiryDay = Calendar.THURSDAY,
        quantity = 50
      )

extension [R, E <: Throwable, A](zio: ZIO[R, E, A])
  def catchAndLog(msg: String) =
    zio.catchAll(e => ZIO.logError(s"$msg: ${e.getMessage}").unit)
