package dev.kpritam.zerodha.kite.login

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.User
import dev.kpritam.zerodha.kite.KiteConfig
import zio.*

trait KiteLogin:
  def getRequestToken: Task[String]
  def createSession: Task[User]

object KiteLogin:
  val live = ZLayer.fromFunction(KiteLoginLive.apply)

  def getRequestToken: RIO[KiteLogin, String] =
    ZIO.serviceWithZIO[KiteLogin](_.getRequestToken)

  def createSession: RIO[KiteLogin, User] =
    ZIO.serviceWithZIO[KiteLogin](_.createSession)

case class KiteLoginLive(kiteConnect: KiteConnect, kiteConfig: KiteConfig) extends KiteLogin:
  def getRequestToken: Task[String] =
    Console.printLine(kiteConnect.getLoginURL) *>
      Console.readLine

  def createSession: Task[User] =
    for {
      requestToken <- getRequestToken
      user         <- ZIO.attemptBlocking(kiteConnect.generateSession(requestToken, kiteConfig.apiSecret))
      _            <- ZIO.succeed(kiteConnect.setAccessToken(user.accessToken))
    } yield user
