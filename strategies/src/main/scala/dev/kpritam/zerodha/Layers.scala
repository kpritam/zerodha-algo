package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.User
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.KiteConfig
import dev.kpritam.zerodha.kite.login.KiteLogin
import sttp.client3.FollowRedirectsBackend
import sttp.client3.HttpClientSyncBackend
import zio.*

//noinspection TypeAnnotation
object Layers:
  val kiteConnectLive =
    ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey, false))

  // zerodha kite login flow requires cookies to be passed along in redirects
  val sttpBackend = ZLayer.succeed(
    new FollowRedirectsBackend(delegate = HttpClientSyncBackend(), sensitiveHeaders = Set())
  )

  val userSession =
    KiteLogin.login.tap(user => ZIO.logInfo(s"${user.userName} logged in successfully."))

  val kiteTickerLive =
    ZLayer.scoped(
      ZIO.acquireRelease(
        for
          user   <- userSession
          ticker <- ZIO.succeed(KiteTicker(user.accessToken, user.apiKey))
        yield ticker
      )(ticker => ZIO.succeed(ticker.disconnect()))
    )
