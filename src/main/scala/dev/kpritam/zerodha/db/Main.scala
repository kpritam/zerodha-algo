package dev.kpritam.zerodha.db

import org.sqlite.SQLiteJDBCLoader
import io.getquill.*
import zio.*
import QuillCtx.*

case class Person(name: String | Null, age: Double)

object Main extends ZIOAppDefault:
  var age               = 30.0
  inline def insertJohn = query[Person].filter(_.age == lift(age))

  def run =
    for
      jhon <- QuillCtx.run(insertJohn).provideLayer(QuillCtx.dataSourceLayer)
      _    <- Console.printLine(jhon)
    yield ()
