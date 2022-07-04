package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig}
import dev.kpritam.zerodha.kite.login.{KiteLogin, Totp}
import dev.kpritam.zerodha.kite.models.Exchange
import zio.*

import java.security.Key
import java.util.Date
import java.time.Instant
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import sttp.client3.*

object App extends ZIOAppDefault:
  private val kiteConnectLive = ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey))

  // zerodha kite login flow requires cookies to be passed along in redirects
  private val backend = ZLayer.succeed(
    new FollowRedirectsBackend(delegate = HttpClientSyncBackend(), sensitiveHeaders = Set())
  )

  def run: ZIO[Any, Throwable, Unit] =
    program
      .provide(
        KiteConfig.live,
        kiteConnectLive,
        KiteLogin.live,
        KiteClient.live,
        Totp.live,
        backend
      )

  private def program =
    for
      requestToken <- KiteLogin.login
      user         <- KiteLogin.createSession(requestToken)
      _            <- Console.printLine(s"${user.userName} logged in successfully.")
//      instruments  <- KiteClient.getInstruments(Exchange("NFO"), "NIFTY", Date.from(Instant.now))
      instruments  <- KiteClient.getInstruments(Exchange("NFO"))
      _            <- ZIO.foreachDiscard(instruments.take(10))(i => Console.printLine(i))
    yield ()
