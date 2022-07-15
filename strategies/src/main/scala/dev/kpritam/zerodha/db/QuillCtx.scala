package dev.kpritam.zerodha.db

import io.getquill.*
import io.getquill.context.ZioJdbc.DataSourceLayer
import zio.*

import javax.sql.DataSource

object QuillCtx extends SqliteZioJdbcContext(NamingStrategy(SnakeCase, Escape)):
  val dataSourceLayer: ULayer[DataSource] =
    DataSourceLayer.fromPrefix("database").orDie
