package dev.kpritam.zerodha.db

import dev.kpritam.zerodha.kite.models.Instrument
import dev.kpritam.zerodha.kite.time
import dev.kpritam.zerodha.kite.time.indiaZone
import zio.*
import zio.test.Assertion.*
import zio.test.*

import java.time.LocalDate
import java.time.LocalDateTime

val instrument1 = Instrument(
  1,
  1,
  "NIFTY",
  "NIFTY50",
  1412.95,
  1,
  "CE",
  "NFO-OPT",
  "NFO",
  0.5,
  1,
  LocalDate.of(2022, 12, 1),
  LocalDateTime.now(indiaZone)
)

val instrument2 = instrument1.copy(instrumentToken = 2, exchangeToken = 2)
val instrument3 = instrument1.copy(instrumentToken = 3, exchangeToken = 3)
val instrument4 = instrument1.copy(instrumentToken = 4, exchangeToken = 4)

object InstrumentsSpec extends ZIOSpecDefault:
  def spec = {
    test("Instruments Repo - CRUD")(
      for
        _                     <- Instruments.create(instrument1)
        _                     <- Instruments.create(List(instrument2, instrument3))
        instrumentsBeforeSeed <- Instruments.all
        _                     <- Instruments.seed(List(instrument3, instrument4))
        instrumentsAfterSeed  <- Instruments.all
      yield assertTrue(instrumentsBeforeSeed.size == 3 && instrumentsAfterSeed.size == 2)
    ) @@ TestAspect.before(Migrations.reset)
  }.provideShared(QuillCtx.dataSourceLayer, Migrations.live, Instruments.live)
