package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import com.zerodhatech.models.User
import com.zerodhatech.ticker
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.models.{Order, QuoteRequest}
import zio.*
import zio.stream.*

import java.lang
import scala.jdk.CollectionConverters.{IterableHasAsJava, SeqHasAsJava}

trait KiteTickerClient:
  def init: Task[Unit]
  def subscribe(tokens: List[lang.Long]): UStream[Order]
  def shutdown: UIO[Unit]

object KiteTickerClient:
  val live = ZLayer.fromFunction(KiteTickerLive.apply)

  def live(user: User): ULayer[KiteTickerClient] = ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.succeed(KiteTickerLive(KiteTicker(user.accessToken, user.apiKey)))
    )(_.shutdown)
  )

  def init: RIO[KiteTickerClient, Unit] =
    ZIO.serviceWithZIO[KiteTickerClient](_.init)

  def subscribeOrders(tokens: List[lang.Long]): ZStream[KiteTickerClient, Nothing, Order] =
    ZStream.serviceWithStream[KiteTickerClient](_.subscribe(tokens))

case class KiteTickerLive(kiteTicker: KiteTicker) extends KiteTickerClient:
  def init: Task[Unit] =
    for
      _ <- ZIO.logDebug("Initializing Kite Ticker")
      _ <- (onConnected <&> onDisconnected <&> onError).fork
      _ <- ZIO.attemptBlocking(kiteTicker.connect())
    yield ()

  def subscribe(tokens: List[lang.Long]): UStream[Order] =
    kiteTicker.subscribe(java.util.ArrayList[lang.Long](tokens.asJava))
    ZStream
      .async { cb =>
        kiteTicker.setOnOrderUpdateListener(zOrder => cb(ZIO.succeed(Chunk(Order.from(zOrder)))))
      }

  def shutdown: UIO[Unit] = ZIO.succeed(kiteTicker.disconnect())

  private def onDisconnected =
    ZStream
      .async[Any, Nothing, Unit](cb =>
        kiteTicker.setOnDisconnectedListener(() =>
          cb(ZIO.logError("Disconnected from Kite Ticker").as(Chunk.empty))
        )
      )
      .runDrain

  private def onConnected =
    ZStream
      .async[Any, Nothing, Unit](cb =>
        kiteTicker.setOnConnectedListener(() =>
          cb(ZIO.logDebug("Connected to Kite Ticker").as(Chunk.empty))
        )
      )
      .runDrain

  private def onError =
    ZStream
      .async[Any, Nothing, Unit](cb =>
        new ticker.OnError {
          println("INIT OnError")
          override def onError(error: String): Unit =
            cb(ZIO.logError(s"Kite Ticker onError: $error)").as(Chunk.empty))

          override def onError(error: Exception): Unit =
            cb(ZIO.logErrorCause("Kite Ticker onError", Cause.fail(error)).as(Chunk.empty))

          override def onError(error: KiteException): Unit =
            cb(ZIO.logErrorCause("Kite Ticker onError", Cause.fail(error)).as(Chunk.empty))
        }
      )
      .runDrain
