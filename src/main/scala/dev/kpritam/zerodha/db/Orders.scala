package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.{Instrument, Order}
import zio.*
import io.getquill.{updateValue, *}
import QuillCtx.*

import java.sql.SQLException
import javax.sql.DataSource

val pendingStatus = "TRIGGER PENDING"
val slOrderType   = "SL"

trait Orders:
  def get(orderId: String): ZIO[DataSource, SQLException, Option[Order]]
  def getPendingSL: ZIO[DataSource, SQLException, List[Order]]

  def create(order: Order): ZIO[DataSource, SQLException, Long]
  def update(order: Order): ZIO[DataSource, SQLException, Long]

case class OrdersLive() extends Orders:
  import QuillCtx.*

  def get(orderId: String): ZIO[DataSource, SQLException, Option[Order]] =
    run(query[Order].filter(_.orderId == lift(orderId))).map(_.headOption)

  def getPendingSL: ZIO[DataSource, SQLException, List[Order]] =
    run(
      query[Order].filter(o => o.status == lift(pendingStatus) && o.orderType == lift(slOrderType))
    )

  def create(order: Order): ZIO[DataSource, SQLException, Long] =
    run(query[Order].insertValue(lift(order)))

  def update(order: Order): ZIO[DataSource, SQLException, Long] =
    run(query[Order].filter(_.orderId == lift(order.orderId)).updateValue(lift(order)))
