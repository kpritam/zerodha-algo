package dev.kpritam.zerodha

import java.util.Calendar

import dev.kpritam.zerodha.Layers
import dev.kpritam.zerodha.db.{Instruments, Orders}
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig, KiteService, KiteTickerClient}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.strategies.everyday.EverydayStrategy

import zio.*

object App extends ZIOAppDefault:

  def run: ZIO[Any, Any, Any] =
    program
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

  private def program =
    EverydayStrategy
      .sellBuyModifyOrder(
        exchange = Exchange("NFO"),
        name = "NIFTY",
        expiryDay = Calendar.THURSDAY,
        quantity = 50
      )
