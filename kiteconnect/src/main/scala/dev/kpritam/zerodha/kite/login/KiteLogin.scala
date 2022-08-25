package dev.kpritam.zerodha.kite.login

import com.zerodhatech.models.User
import zio.*

trait KiteLogin:
  def requestToken: Task[String]
  def loginManual: Task[String]
  def login: Task[User]
  def createSession(requestToken: String): Task[User]

  def logout: Task[Unit]

object KiteLogin:

  def requestToken: RIO[KiteLogin, String] =
    ZIO.serviceWithZIO[KiteLogin](_.requestToken)

  def loginManual: RIO[KiteLogin, String] =
    ZIO.serviceWithZIO[KiteLogin](_.loginManual)

  def createSession(requestToken: String): RIO[KiteLogin, User] =
    ZIO.serviceWithZIO[KiteLogin](_.createSession(requestToken))

  def login: RIO[KiteLogin, User] =
    ZIO.serviceWithZIO[KiteLogin](_.login)
