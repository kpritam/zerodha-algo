package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import zio.*

import java.sql.SQLException

trait Instruments:
  def seed(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def all: IO[SQLException, List[Instrument]]

  def create(instrument: Instrument): IO[SQLException, Long]
  def create(instruments: List[Instrument]): IO[SQLException, List[Long]]

  def delete: IO[SQLException, Long]

object Instruments:

  def seed(instruments: List[Instrument]): ZIO[Instruments, SQLException, List[Long]] =
    ZIO.serviceWithZIO[Instruments](_.seed(instruments))

  def create(instrument: Instrument): ZIO[Instruments, SQLException, Long] =
    ZIO.serviceWithZIO[Instruments](_.create(instrument))

  def create(instruments: List[Instrument]): ZIO[Instruments, SQLException, List[Long]] =
    ZIO.serviceWithZIO[Instruments](_.create(instruments))

  def all: ZIO[Instruments, SQLException, List[Instrument]] =
    ZIO.serviceWithZIO[Instruments](_.all)
