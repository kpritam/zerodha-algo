package dev.kpritam.zerodha

import com.zerodhatech.kiteconnect.KiteConnect
import dev.kpritam.zerodha.kite.{KiteClient, KiteConfig}
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.models.Exchange
import zio.*

import java.util.Date
import java.time.Instant

object App extends ZIOAppDefault:
  private val kiteConnectLive = ZLayer.fromFunction((cfg: KiteConfig) => KiteConnect(cfg.apiKey))

  def run: ZIO[Any, Throwable, Unit] =
    program.provide(
      KiteConfig.live,
      kiteConnectLive,
      KiteLogin.live,
      KiteClient.live
    )

  private def program =
    for
      user        <- KiteLogin.createSession
      _           <- Console.printLine(s"${user.userName} logged in successfully.")
      instruments <- KiteClient.getInstruments(Exchange("NFO"), "NIFTY", Date.from(Instant.now))
      _           <- ZIO.foreachDiscard(instruments.take(10))(i => Console.printLine(i.name))
    yield ()
