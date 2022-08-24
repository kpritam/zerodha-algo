package dev.kpritam.zerodha.db

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

object QuillCtx extends SqliteZioJdbcContext(NamingStrategy(SnakeCase, Escape)):
  val dataSourceLayer: ULayer[DataSource] =
    Quill.DataSource.fromPrefix("database").orDie
