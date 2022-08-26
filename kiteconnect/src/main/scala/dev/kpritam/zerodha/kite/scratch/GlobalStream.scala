package dev.kpritam.zerodha.kite.scratch

import zio.*
import zio.stream.*

class GlobalStream(globalStream: UStream[Int]):
  def subscribe(token: Int) =
    globalStream.filter(_ == token).debug(s"SUB[$token]")

object Main extends ZIOAppDefault:

  private def program =
    for
      gs          <- ZStream
                       .range(0, 10)
                       .schedule(Schedule.spaced(1.seconds))
                       .debug("Global Stream")
                       .broadcastDynamic(1)
      globalStream = GlobalStream(gs)
      f1          <- globalStream.subscribe(5).runDrain.fork
      f2          <- globalStream.subscribe(7).runDrain.fork
      _           <- f1.zip(f2).await
    yield ()

  def run = program
