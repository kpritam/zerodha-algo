package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.db.{Instruments, Orders}
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig, KiteService, KiteTickerClient}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.{Exchange, InstrumentRequest}
import dev.kpritam.zerodha.strategies.everyday.everyday
import dev.kpritam.zerodha.time.nextWeekday
import zio.*
import zio.logging

import java.security.Key
import java.util.{Calendar, Date}
import java.time.{DayOfWeek, Instant}
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import sttp.client3.*
import zio.ZLayer.Debug
import zio.json.EncoderOps

object App extends ZIOAppDefault:
  private val kiteConnectLive =
    ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey))

  // zerodha kite login flow requires cookies to be passed along in redirects
  private val sttpBackend = ZLayer.succeed(
    new FollowRedirectsBackend(delegate = HttpClientSyncBackend(), sensitiveHeaders = Set())
  )

  def run: ZIO[Any, Any, Any] =
    program
      .provide(
        logging.console(logLevel = LogLevel.All),
        Totp.live,
        sttpBackend,
        // kite
        kiteConnectLive,
        KiteConfig.live,
        KiteLogin.live,
        KiteClient.live,
        // service
        KiteService.live,
        // db
        Instruments.live,
        Orders.live
      )

  private def program = everyday.cause.debug
