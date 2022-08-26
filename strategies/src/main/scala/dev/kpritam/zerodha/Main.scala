package dev.kpritam.zerodha

import dev.kpritam.zerodha.kite.KiteClient
import dev.kpritam.zerodha.kite.KiteConfig
import dev.kpritam.zerodha.kite.login.KiteLogin
import dev.kpritam.zerodha.kite.login.KiteLoginLive
import dev.kpritam.zerodha.kite.login.Totp
import dev.kpritam.zerodha.kite.models.Exchange
import dev.kpritam.zerodha.kite.models.QuoteRequest
import dev.kpritam.zerodha.kite.models.TradingSymbol
import zio.*

import java.time.Instant
import java.util.Date

object Main extends ZIOAppDefault:

  def run =
    (for
      quotes <-
        KiteClient.getOHLC(QuoteRequest.Instrument(TradingSymbol("NIFTY 50"), Exchange("NSE")))
      hd     <- KiteClient.getHistoricalData(
                  Date.from(Instant.parse("2022-08-25T09:30:00Z")),
                  Date.from(Instant.parse("2022-08-25T09:45:00Z")),
                  QuoteRequest.Instrument(TradingSymbol("NIFTY 50"), Exchange("NSE")).instrument,
                  "15m"
                )
      _      <- Console.printLine(quotes)
      _      <- Console.printLine(hd)
    yield ())
      .provide(
        Totp.live,
        Layers.sttpBackend,
        Layers.kiteConnectLive,
        Layers.kiteTickerLive,
        KiteConfig.live,
        KiteLoginLive.layer,
        KiteClient.live
      )
