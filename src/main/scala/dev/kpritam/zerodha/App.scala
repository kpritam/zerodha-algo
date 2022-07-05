package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig, KiteService}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.{Exchange, InstrumentRequest}
import dev.kpritam.zerodha.time.nextWeekday
import zio.*

import java.security.Key
import java.util.{Calendar, Date}
import java.time.{DayOfWeek, Instant}
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import sttp.client3.*
import zio.json.EncoderOps

val nfo               = Exchange("NFO")
val nifty             = "NIFTY"
val expiryDay         = nextWeekday(Calendar.THURSDAY)
val instrumentRequest = InstrumentRequest(nfo, nifty, expiryDay)
val price             = 12

object App extends ZIOAppDefault:
  private val kiteConnectLive = ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey))

  // zerodha kite login flow requires cookies to be passed along in redirects
  private val sttpBackend = ZLayer.succeed(
    new FollowRedirectsBackend(delegate = HttpClientSyncBackend(), sensitiveHeaders = Set())
  )

  def run: ZIO[Any, Throwable, Unit] =
    program
      .provide(
        KiteConfig.live,
        kiteConnectLive,
        KiteLogin.live,
        KiteClient.live,
        KiteService.live,
        Totp.live,
        sttpBackend
      )

  private def program =
    for
      requestToken <- KiteLogin.login
      user         <- KiteLogin.createSession(requestToken)
      _            <- Console.printLine(s"${user.userName} logged in successfully.")
      cepe         <- KiteService.getCEPEInstrument(instrumentRequest, price)
      _            <- Console.printLine(cepe.toJson)
    yield ()
