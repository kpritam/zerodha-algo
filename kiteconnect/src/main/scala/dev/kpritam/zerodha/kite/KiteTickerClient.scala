package dev.kpritam.zerodha.kite

import dev.kpritam.zerodha.kite.models.*
import zio.*
import zio.stream.*

import java.lang

trait KiteTickerClient:
  def subscribe(tokens: List[lang.Long]): UStream[Order]

object KiteTickerClient:

  def subscribe(tokens: List[lang.Long]): ZStream[KiteTickerClient, Nothing, Order] =
    ZStream.serviceWithStream[KiteTickerClient](_.subscribe(tokens))
