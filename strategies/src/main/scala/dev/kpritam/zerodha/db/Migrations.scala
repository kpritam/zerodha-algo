package dev.kpritam.zerodha.db

import org.flywaydb.core.Flyway
import zio.*

import javax.sql.DataSource

final case class Migrations(dataSource: DataSource):

  val migrate: Task[Unit] =
    for
      flyway <- loadFlyway
      _      <- ZIO.attempt(flyway.migrate())
    yield ()

  val reset: Task[Unit] =
    for
      _      <- ZIO.logDebug("RESETTING DATABASE!")
      flyway <- loadFlyway
      _      <- ZIO.attempt(flyway.clean())
      _      <- ZIO.attempt(flyway.migrate())
    yield ()

  private lazy val loadFlyway: Task[Flyway] =
    ZIO.attempt {
      Flyway
        .configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .cleanDisabled(false)
        .load()
    }

object Migrations:
  val live: URLayer[DataSource, Migrations] = ZLayer.fromFunction(Migrations.apply _)

  def migrate: ZIO[Migrations, Throwable, Unit] =
    ZIO.serviceWithZIO[Migrations](_.migrate)

  def reset: ZIO[Migrations, Throwable, Unit] =
    ZIO.serviceWithZIO[Migrations](_.reset)
