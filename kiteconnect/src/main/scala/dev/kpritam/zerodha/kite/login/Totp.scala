package dev.kpritam.zerodha.kite.login

import dev.kpritam.zerodha.kite.KiteConfig
import dev.samstevens.totp.code.DefaultCodeGenerator
import zio.*

import java.time.Instant
import javax.crypto.spec.SecretKeySpec

trait Totp:
  def generate: Task[String]

object Totp:
  val live = ZLayer.fromFunction(TotpLive.apply)

  def generate: RIO[Totp, String] = ZIO.serviceWithZIO(_.generate)

case class TotpLive(kiteConfig: KiteConfig) extends Totp:
  private val bucket = 30
  private val otpGen = DefaultCodeGenerator()

  def generate: Task[String] =
    for
      now <- Clock.instant
      otp <-
        ZIO.attempt(otpGen.generate(kiteConfig.totpSecret, now.getEpochSecond / bucket))
    yield otp
