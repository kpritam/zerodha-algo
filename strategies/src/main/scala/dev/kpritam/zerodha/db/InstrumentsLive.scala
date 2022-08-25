package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import io.getquill.*
import zio.*

import java.sql.SQLException
import javax.sql.DataSource

case class InstrumentsLive(dataSource: DataSource) extends Instruments:
  import QuillCtx.*

  def create(instrument: Instrument): IO[SQLException, Long] =
    run(query[Instrument].insertValue(lift(instrument)))
      .provideEnvironment(ZEnvironment(dataSource))

  def create(instruments: List[Instrument]): IO[SQLException, List[Long]] =
    run(liftQuery(instruments).foreach(e => query[Instrument].insertValue(e)))
      .provideEnvironment(ZEnvironment(dataSource))

  def all: IO[SQLException, List[Instrument]] =
    run(query[Instrument]).provideEnvironment(ZEnvironment(dataSource))

  def delete: IO[SQLException, Long] =
    run(query[Instrument].delete).provideEnvironment(ZEnvironment(dataSource))

  def seed(instruments: List[Instrument]): IO[SQLException, List[Long]] =
    delete *> create(instruments)

object InstrumentsLive:
  val layer: URLayer[DataSource, Instruments] = ZLayer.fromFunction(InstrumentsLive.apply)
