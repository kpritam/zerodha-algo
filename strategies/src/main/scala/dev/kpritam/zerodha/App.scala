package dev.kpritam.zerodha

import dev.kpritam.zerodha.Layers
import dev.kpritam.zerodha.cron.*
import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.db.Orders
import dev.kpritam.zerodha.kite.KiteClient
import dev.kpritam.zerodha.kite.KiteConfig
import dev.kpritam.zerodha.kite.KiteService
import dev.kpritam.zerodha.kite.KiteTickerClient
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.login.Totp
import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.time.indiaZone
import dev.kpritam.zerodha.strategies.everyday.EverydayStrategy
import dev.kpritam.zerodha.strategies.hedge.OvernightHedge
import zio.*

import java.time.LocalDateTime
import java.util.Calendar

object App extends ZIOAppDefault:

  def run: ZIO[Any, Any, Any] =
    (for
      _ <- ZIO.logInfo("Scheduling app to run at 9:20 AM")
      _ <- app.schedule(everyday(9, 20))
    yield ()).provideSome(
      logging.removeDefaultLoggers,
      logging.console(logLevel = LogLevel.All)
    )

  private val app =
    (for
      _ <- ZIO.logInfo("Starting app ...")
      _ <- KiteTickerClient.init
      _ <- seedInstrumentsIfNeeded
      _ <- strategies.everyday.run
    yield ())
      .provide(
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
        EverydayStrategy.live,
        ZLayer.Debug.tree
      )
