package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import zio.*
import io.getquill.*

import java.sql.SQLException
import javax.sql.DataSource

trait Instruments:
  def seed(instruments: List[Instrument]): ZIO[DataSource, SQLException, List[Long]]

  def all: ZIO[DataSource, SQLException, List[Instrument]]

  def create(instrument: Instrument): ZIO[DataSource, SQLException, Long]
  def create(instruments: List[Instrument]): ZIO[DataSource, SQLException, List[Long]]

  def delete: ZIO[DataSource, SQLException, Long]

case class InstrumentsLive() extends Instruments:
  import QuillCtx.*

  def create(instrument: Instrument): ZIO[DataSource, SQLException, Long] =
    run(quote(query[Instrument].insertValue(lift(instrument))))

  def create(instruments: List[Instrument]): ZIO[DataSource, SQLException, List[Long]] =
    run(liftQuery(instruments).foreach(e => query[Instrument].insertValue(e)))

  def all: ZIO[DataSource, SQLException, List[Instrument]] =
    run(query[Instrument])

  def delete: ZIO[DataSource, SQLException, Long] =
    run(query[Instrument].delete)

  def seed(instruments: List[Instrument]): ZIO[DataSource, SQLException, List[Long]] =
    delete *> create(instruments)
