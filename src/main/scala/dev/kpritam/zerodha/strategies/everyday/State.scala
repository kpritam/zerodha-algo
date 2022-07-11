package dev.kpritam.zerodha.strategies.everyday

import dev.kpritam.zerodha.kite.models.Order
import zio.{Ref, UIO}

case class State(
    ceOrder: Option[Order] = None,
    peOrder: Option[Order] = None,
    ceSLOrder: Option[Order] = None,
    peSLOrder: Option[Order] = None
):
  def updateCe(o: Order): State   = copy(ceOrder = Some(o))
  def updatePe(o: Order): State   = copy(peOrder = Some(o))
  def updateCeSL(o: Order): State = copy(ceSLOrder = Some(o))
  def updatePeSL(o: Order): State = copy(peSLOrder = Some(o))

object State:
  def make: UIO[Ref[State]] = Ref.make(State())

extension (s: Ref[State])
  def updateCe(o: Order): UIO[Unit]   = s.update(_.updateCe(o))
  def updatePe(o: Order): UIO[Unit]   = s.update(_.updatePe(o))
  def updateCeSL(o: Order): UIO[Unit] = s.update(_.updateCeSL(o))
  def updatePeSL(o: Order): UIO[Unit] = s.update(_.updatePeSL(o))
