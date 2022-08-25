package dev.kpritam.zerodha.kite.login

import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.User
import dev.kpritam.zerodha.kite.KiteConfig
import sttp.client3.*
import sttp.client3.ziojson.asJson
import sttp.model.headers.CookieWithMeta
import zio.*

private val baseURL         = "https://kite.zerodha.com"
private val loginURL        = baseURL + "/api/login"
private val twofaURL        = baseURL + "/api/twofa"
private val connectLoginURL = baseURL + "/connect/login"

private def extractRequestToken(url: String): Option[String] =
  url.split("request_token=").lastOption.flatMap(_.split("&").headOption)

case class KiteLoginLive(
    kiteConnect: KiteConnect,
    kiteConfig: KiteConfig,
    totp: Totp,
    backend: SttpBackend[Identity, Any]
) extends KiteLogin:
  def requestToken: Task[String] =
    for
      loginRes     <- post[LoginResponse](loginURL, loginRequest)
      loginResBody <- ZIO.fromEither(loginRes.body)
      twofaBody    <- twofaRequest(loginResBody.data.request_id)
      twofaRes     <- post[TwofaResponse](twofaURL, twofaBody)
      requestToken <- get[TwofaResponse](
                        s"$connectLoginURL?api_key=${kiteConfig.apiKey}&v=3",
                        loginRequest,
                        twofaRes.unsafeCookies
                      ).foldZIO(
                        e => ZIO.fromOption(extractRequestToken(e.getMessage)).orElseFail(e),
                        r => ZIO.fail(RuntimeException(s"failed to get request_token: $r"))
                      )
    yield requestToken

  def loginManual: Task[String] =
    ZIO.logInfo(kiteConnect.getLoginURL) *> Console.readLine

  def createSession(requestToken: String): Task[User] =
    for {
      user <- ZIO.attemptBlocking(kiteConnect.generateSession(requestToken, kiteConfig.apiSecret))
      _    <- ZIO.succeed(kiteConnect.setAccessToken(user.accessToken))
    } yield user

  def login: Task[User] =
    requestToken.flatMap(createSession)

  def logout: Task[Unit] =
    ZIO.attemptBlocking(kiteConnect.logout())

  private def post[T: zio.json.JsonDecoder](
      url: String,
      body: Map[String, String],
      cookies: Iterable[CookieWithMeta] = Nil
  ) =
    ZIO.attemptBlocking(
      basicRequest.cookies(cookies).body(body).post(uri"$url").response(asJson[T]).send(backend)
    )

  private def get[T: zio.json.JsonDecoder](
      url: String,
      body: Map[String, String],
      cookies: Iterable[CookieWithMeta] = Nil
  ) =
    ZIO.attemptBlocking {
      basicRequest.cookies(cookies).body(body).get(uri"$url").response(asJson[T]).send(backend)
    }

  private def twofaRequest(requestId: String) =
    for otp <- totp.generate
    yield Map(
      "user_id"      -> kiteConfig.userId,
      "request_id"   -> requestId,
      "twofa_value"  -> otp,
      "skip_session" -> "true"
    )

  private val loginRequest = Map("user_id" -> kiteConfig.userId, "password" -> kiteConfig.password)

object KiteLoginLive:
  val layer = ZLayer.fromFunction(KiteLoginLive.apply)
