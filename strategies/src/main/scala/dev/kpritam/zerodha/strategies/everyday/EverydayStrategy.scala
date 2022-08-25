package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.*
import zio.*

trait EverydayStrategy:
  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): Task[Unit]

  def modifyPendingOrders: Task[List[String]]
  def closePendingOrders: Task[List[String]]

object EverydayStrategy:

  def sellBuyModifyOrder(
      exchange: Exchange,
      name: String,
      expiryDay: Int,
      quantity: Int
  ): RIO[EverydayStrategy, Unit] =
    ZIO.serviceWithZIO[EverydayStrategy](_.sellBuyModifyOrder(exchange, name, expiryDay, quantity))

  def modifyPendingOrders: RIO[EverydayStrategy, List[String]] =
    ZIO.serviceWithZIO[EverydayStrategy](_.modifyPendingOrders)

  def closePendingOrders: RIO[EverydayStrategy, List[String]] =
    ZIO.serviceWithZIO[EverydayStrategy](_.closePendingOrders)
