package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.models.Order
import io.getquill.*
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
  val live: URLayer[DataSource, OrdersLive] = ZLayer.fromFunction(OrdersLive.apply)

  def create(order: Order): ZIO[Orders, SQLException, Long] =
    ZIO.serviceWithZIO[Orders](_.create(order))

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
