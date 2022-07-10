package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import zio.*
import io.getquill.*

import java.sql.SQLException

trait Instruments:
  def seed(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def all: IO[SQLException, List[Instrument]]

  def create(instrument: Instrument): IO[SQLException, Long]
  def create(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def delete: IO[SQLException, Long]

object Instruments:
  val live: ULayer[InstrumentsLive] = ZLayer.succeed(InstrumentsLive())

case class InstrumentsLive() extends Instruments:
  import QuillCtx.*

  def create(instrument: Instrument): IO[SQLException, Long] =
    run(quote(query[Instrument].insertValue(lift(instrument)))).provide(dataSourceLayer)

  def create(instruments: List[Instrument]): IO[SQLException, List[Long]] =
    run(liftQuery(instruments).foreach(e => query[Instrument].insertValue(e)))
      .provide(dataSourceLayer)

  def all: IO[SQLException, List[Instrument]] =
    run(query[Instrument]).provide(dataSourceLayer)

  def delete: IO[SQLException, Long] =
    run(query[Instrument].delete).provide(dataSourceLayer)

  def seed(instruments: List[Instrument]): IO[SQLException, List[Long]] =
    delete *> create(instruments)
