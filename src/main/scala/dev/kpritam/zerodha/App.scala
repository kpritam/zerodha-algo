package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.db.Instruments
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig, KiteService, KiteTickerClient}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.{Exchange, InstrumentRequest}
import dev.kpritam.zerodha.strategies.everyday.strategy
import dev.kpritam.zerodha.time.nextWeekday
import zio.*

import java.security.Key
import java.util.{Calendar, Date}
import java.time.{DayOfWeek, Instant}
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import sttp.client3.*
import zio.ZLayer.Debug
import zio.json.EncoderOps

object App extends ZIOAppDefault:
  private val kiteConnectLive = ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey))

  // zerodha kite login flow requires cookies to be passed along in redirects
  private val sttpBackend = ZLayer.succeed(
    new FollowRedirectsBackend(delegate = HttpClientSyncBackend(), sensitiveHeaders = Set())
  )

  def run =
    program
      .provide(
        KiteConfig.live,
        kiteConnectLive,
        KiteLogin.live,
        KiteClient.live,
        KiteService.live,
        Totp.live,
        sttpBackend,
        Instruments.live
      )

  private def program = strategy.cause.debug
