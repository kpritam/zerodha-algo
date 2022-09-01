package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.Order
import zio.Ref
import zio.UIO

case class State(
    ceOrder: Option[Order] = None,
    peOrder: Option[Order] = None,
    ceSLOrder: Option[Order] = None,
    peSLOrder: Option[Order] = None
):
  val ceOrderId: Option[String]   = ceOrder.map(_.orderId)
  val peOrderId: Option[String]   = peOrder.map(_.orderId)
  val ceSLOrderId: Option[String] = ceSLOrder.map(_.orderId)
  val peSLOrderId: Option[String] = peSLOrder.map(_.orderId)

  def updateCe(o: Order): State   = copy(ceOrder = Some(o))
  def updatePe(o: Order): State   = copy(peOrder = Some(o))
  def updateCeSL(o: Order): State = copy(ceSLOrder = Some(o))
  def updatePeSL(o: Order): State = copy(peSLOrder = Some(o))

  def debug: String =
    "[ " +
      orderStr("CE Order", ceOrder) + ", " +
      orderStr("PE Order", peOrder) + ", " +
      orderStr("CE SL Order", ceSLOrder) + ", " +
      orderStr("PE SL Order", peSLOrder) + ", " +
      " ]"

  private def orderStr(name: String, order: Option[Order]) = s"$name: (${order.fold("")(_.debug)})"

object State:
  def make: UIO[Ref[State]] = Ref.make(State())

extension (s: Ref[State])
  def updateCe(o: Order): UIO[Unit]   = s.update(_.updateCe(o))
  def updatePe(o: Order): UIO[Unit]   = s.update(_.updatePe(o))
  def updateCeSL(o: Order): UIO[Unit] = s.update(_.updateCeSL(o))
  def updatePeSL(o: Order): UIO[Unit] = s.update(_.updatePeSL(o))
