package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.models.Order
import io.getquill._
import io.getquill.updateValue
import zio.*

import java.sql.SQLException
import javax.sql.DataSource

import QuillCtx.*

val pendingStatus = "TRIGGER PENDING"
val slOrderType   = "SL"

trait Orders:
  def get(orderId: String): IO[SQLException, Option[Order]]
  def getPendingSL: IO[SQLException, List[Order]]

  def create(order: Order): IO[SQLException, Long]
  def update(order: Order): IO[SQLException, Long]

object Orders:
  val live: ULayer[OrdersLive] = ZLayer.succeed(OrdersLive())

  def create(order: Order): ZIO[Orders, SQLException, Long] =
    ZIO.serviceWithZIO[Orders](_.create(order))

case class OrdersLive() extends Orders:
  import QuillCtx.*

  def get(orderId: String): IO[SQLException, Option[Order]] =
    run(query[Order].filter(_.orderId == lift(orderId))).map(_.headOption).provide(dataSourceLayer)

  def getPendingSL: IO[SQLException, List[Order]] =
    run(
      query[Order].filter(o => o.status == lift(pendingStatus) && o.orderType == lift(slOrderType))
    ).provide(dataSourceLayer)

  def create(order: Order): IO[SQLException, Long] =
    run(query[Order].insertValue(lift(order))).provide(dataSourceLayer)

  def update(order: Order): IO[SQLException, Long] =
    run(query[Order].filter(_.orderId == lift(order.orderId)).updateValue(lift(order)))
      .provide(dataSourceLayer)
