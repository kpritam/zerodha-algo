import zio.*
import zio.stream.*
import java.io.IOException

object App extends ZIOAppDefault:
  val helloStream: UStream[Int] =
    ZStream
      .fromIterable(1 to 200)
      .schedule(Schedule.fixed(1.second))
      .filter(_ % 2 == 0)
      .debug("Filtered")
      .take(4)
      .ensuring(Console.printLine("Goodbye").orDie)

  def run =
    helloStream.runDrain
