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

object KiteTickerClient:
  val live = ZLayer.fromFunction(KiteTickerLive.apply)

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
    val jTokens = java.util.ArrayList[lang.Long](tokens.asJava)
    kiteTicker.subscribe(jTokens)
    ZStream
      .async[Any, Nothing, Order] { cb =>
        kiteTicker.setOnOrderUpdateListener(zOrder => cb(ZIO.succeed(Chunk(Order.from(zOrder)))))
      }
      .ensuring(
        ZIO.logDebug(s"Unsubscribing token: ${tokens.mkString(",")}") *> ZIO
          .succeed(kiteTicker.unsubscribe(jTokens))
      )

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
