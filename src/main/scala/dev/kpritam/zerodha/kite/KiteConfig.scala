package dev.kpritam.zerodha.kite

import zio.*
import zio.config.*
import zio.config.magnolia.Descriptor.*
import zio.config.magnolia.*

case class KiteConfig(
    apiKey: String,
    apiSecret: String,
    userId: String,
    password: String,
    totpSecret: String
)

object KiteConfig:
  val live = ZConfig.fromSystemEnv(descriptor[KiteConfig])

  def apiKey: URIO[KiteConfig, String]     = ZIO.serviceWith[KiteConfig](_.apiKey)
  def apiSecret: URIO[KiteConfig, String]  = ZIO.serviceWith[KiteConfig](_.apiSecret)
  def userId: URIO[KiteConfig, String]     = ZIO.serviceWith[KiteConfig](_.userId)
  def password: URIO[KiteConfig, String]   = ZIO.serviceWith[KiteConfig](_.password)
  def totpSecret: URIO[KiteConfig, String] = ZIO.serviceWith[KiteConfig](_.totpSecret)
