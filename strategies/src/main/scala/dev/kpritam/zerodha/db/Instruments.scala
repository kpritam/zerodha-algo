package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import io.getquill.*
import io.getquill.context.qzio.ZioJdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import zio.*

import java.sql.SQLException
import javax.sql.DataSource

trait Instruments:
  def seed(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def all: IO[SQLException, List[Instrument]]

  def create(instrument: Instrument): IO[SQLException, Long]
  def create(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def delete: IO[SQLException, Long]

object Instruments:
  val live: URLayer[DataSource, Instruments] = ZLayer.fromFunction(InstrumentsLive.apply)

  def seed(instruments: List[Instrument]): ZIO[Instruments, SQLException, List[Long]] =
    ZIO.serviceWithZIO[Instruments](_.seed(instruments))

  def create(instrument: Instrument): ZIO[Instruments, SQLException, Long] =
    ZIO.serviceWithZIO[Instruments](_.create(instrument))

  def create(instruments: List[Instrument]): ZIO[Instruments, SQLException, List[Long]] =
    ZIO.serviceWithZIO[Instruments](_.create(instruments))

  def all: ZIO[Instruments, SQLException, List[Instrument]] =
    ZIO.serviceWithZIO[Instruments](_.all)

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
