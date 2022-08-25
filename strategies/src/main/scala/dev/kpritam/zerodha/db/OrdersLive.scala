package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Order
import io.getquill.*
import zio.IO
import zio.URLayer
import zio.ZEnvironment
import zio.ZLayer

import java.sql.SQLException
import javax.sql.DataSource

val pendingStatus = "TRIGGER PENDING"
val slOrderType   = "SL"

case class OrdersLive(dataSource: DataSource) extends Orders:
  import QuillCtx.*

  def get(orderId: String): IO[SQLException, Option[Order]] =
    run(query[Order].filter(_.orderId == lift(orderId)))
      .map(_.headOption)
      .provideEnvironment(ZEnvironment(dataSource))

  def getPendingSL: IO[SQLException, List[Order]] =
    run(
      query[Order].filter(o => o.status == lift(pendingStatus) && o.orderType == lift(slOrderType))
    ).provideEnvironment(ZEnvironment(dataSource))

  def create(order: Order): IO[SQLException, Long] =
    run(query[Order].insertValue(lift(order))).provideEnvironment(ZEnvironment(dataSource))

  def update(order: Order): IO[SQLException, Long] =
    run(query[Order].filter(_.orderId == lift(order.orderId)).updateValue(lift(order)))
      .provideEnvironment(ZEnvironment(dataSource))

object OrdersLive:
  val layer: URLayer[DataSource, OrdersLive] = ZLayer.fromFunction(OrdersLive.apply)
