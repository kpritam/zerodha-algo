package dev.kpritam.zerodha.kite

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.Order
import com.zerodhatech.ticker
import com.zerodhatech.ticker.KiteTicker
import dev.kpritam.zerodha.kite.models.QuoteRequest
import zio.*
import zio.stream.*

import java.lang
import scala.jdk.CollectionConverters.{IterableHasAsJava, SeqHasAsJava}

trait KiteTickerClient:
  def subscribe(tokens: List[lang.Long]): UStream[Order]

object KiteTickerClient:
  val live = ZLayer.fromFunction(KiteTickerLive.apply)

  def subscribe(tokens: List[lang.Long]): ZStream[KiteTickerClient, Nothing, Order] =
    ZStream.serviceWithStream[KiteTickerClient](_.subscribe(tokens))

case class KiteTickerLive(kiteTicker: KiteTicker) extends KiteTickerClient:
  def subscribe(tokens: List[lang.Long]): UStream[Order] =
    stream.ZStream
      .async { cb =>
        kiteTicker.subscribe(java.util.ArrayList[lang.Long](tokens.asJava))
        kiteTicker.setOnOrderUpdateListener { order =>
          cb(ZIO.succeed(Chunk(order)))
        }
      }
