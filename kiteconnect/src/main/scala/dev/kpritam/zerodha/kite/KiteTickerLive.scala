package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import com.zerodhatech.ticker
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.models.Order
import zio.*
import zio.stream.*

import java.lang
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.jdk.CollectionConverters.SeqHasAsJava

private val maxLag = 32

case class KiteTickerLive(kiteTicker: KiteTicker, globalStream: UStream[Order])
    extends KiteTickerClient:

  def subscribe(tokens: List[lang.Long]): UStream[Order] =
    val jTokens = java.util.ArrayList[lang.Long](tokens.asJava)
    kiteTicker.subscribe(jTokens)
    globalStream
      .ensuring(
        ZIO.logDebug(s"Unsubscribing token: ${tokens.mkString(",")}") *> ZIO
          .succeed(kiteTicker.unsubscribe(jTokens))
      )

object KiteTickerLive:

  val layer: ZLayer[KiteTicker, Throwable, KiteTickerLive] = ZLayer.scoped {
    for
      ticker       <- ZIO.service[KiteTicker]
      globalStream <- init(ticker)
    yield KiteTickerLive(ticker, globalStream)
  }

  private def init(kiteTicker: KiteTicker): RIO[Scope, UStream[Order]] =
    for
      _   <- ZIO.logDebug("Initializing Kite Ticker")
      _   <- (onConnected(kiteTicker) <&> onDisconnected(kiteTicker) <&> onError).forkDaemon
      _   <- ZIO.sleep(1.second)
      _   <- ZIO.succeed(kiteTicker.setTryReconnection(true))
      _   <- ZIO.attemptBlocking(kiteTicker.connect())
      _   <- ZIO.logDebug("Creating global subscription")
      hub <- globalSubscription(kiteTicker)
    yield hub

  private def globalSubscription(
      kiteTicker: KiteTicker
  ): ZIO[Scope, Nothing, UStream[Order]] =
    ZStream
      .async[Any, Nothing, Order] { cb =>
        kiteTicker.setOnOrderUpdateListener(zOrder => cb(ZIO.succeed(Chunk(Order.from(zOrder)))))
      }
      .broadcastDynamic(maxLag)

  private def onDisconnected(kiteTicker: KiteTicker) =
    ZStream
      .async[Any, Nothing, Unit](cb =>
        kiteTicker.setOnDisconnectedListener(() =>
          cb(ZIO.logError("Disconnected from Kite Ticker").as(Chunk.empty))
        )
      )
      .runDrain

  private def onConnected(kiteTicker: KiteTicker) =
    ZStream
      .async[Any, Nothing, Unit](cb =>
        kiteTicker.setOnConnectedListener(() =>
          cb(ZIO.logDebug("Connected to Kite Ticker").as(Chunk.empty))
        )
      )
      .runDrain

  private def onError =
    ZStream
      .async[Any, Nothing, Unit] { cb =>
        new ticker.OnError:
          override def onError(error: String): Unit =
            cb(ZIO.logError(s"Kite Ticker onError: $error)").as(Chunk.empty))

          override def onError(error: Exception): Unit =
            cb(ZIO.logErrorCause("Kite Ticker onError", Cause.fail(error)).as(Chunk.empty))

          override def onError(error: KiteException): Unit =
            cb(ZIO.logErrorCause("Kite Ticker onError", Cause.fail(error)).as(Chunk.empty))
      }
      .runDrain
