package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Order
import zio.*

import java.sql.SQLException

trait Orders:
  def get(orderId: String): IO[SQLException, Option[Order]]
  def getPendingSL: IO[SQLException, List[Order]]

  def create(order: Order): IO[SQLException, Long]
  def update(order: Order): IO[SQLException, Long]

object Orders:

  def get(orderId: String): ZIO[Orders, SQLException, Option[Order]] =
    ZIO.serviceWithZIO[Orders](_.get(orderId))

  def getPendingSL: ZIO[Orders, SQLException, List[Order]] =
    ZIO.serviceWithZIO[Orders](_.getPendingSL)

  def create(order: Order): ZIO[Orders, SQLException, Long] =
    ZIO.serviceWithZIO[Orders](_.create(order))

  def update(order: Order): ZIO[Orders, SQLException, Long] =
    ZIO.serviceWithZIO[Orders](_.update(order))
