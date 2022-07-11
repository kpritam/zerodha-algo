package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import com.zerodhatech.ticker
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.models.{Order, QuoteRequest}
import zio.*
import zio.stream.*

import java.lang
import scala.jdk.CollectionConverters.{IterableHasAsJava, SeqHasAsJava}

trait KiteTickerClient:
  def subscribe(tokens: List[lang.Long]): UStream[Order]

object KiteTickerClient:
  val live = ZLayer.fromFunction(KiteTickerLive.apply)

  def subscribeOrders(tokens: List[lang.Long]): ZStream[KiteTickerClient, Nothing, Order] =
    ZStream.serviceWithStream[KiteTickerClient](_.subscribe(tokens))

case class KiteTickerLive(kiteTicker: KiteTicker) extends KiteTickerClient:
  kiteTicker.setOnConnectedListener(() => println("[ws] Connected"))
  kiteTicker.setOnDisconnectedListener(() => println("[ws] Disconnected"))
  kiteTicker.setOnErrorListener(new ticker.OnError {
    override def onError(error: String): Unit        = println(s"[ws] Error1: $error")
    override def onError(error: Exception): Unit     = println(s"[ws] Error2: $error")
    override def onError(error: KiteException): Unit = println(s"[ws] Error3: $error")
  })
  kiteTicker.connect()

  def subscribe(tokens: List[lang.Long]): UStream[Order] =
    kiteTicker.subscribe(java.util.ArrayList[lang.Long](tokens.asJava))
    ZStream
      .async { cb =>
        kiteTicker.setOnOrderUpdateListener(zOrder => cb(ZIO.succeed(Chunk(Order.from(zOrder)))))
      }
