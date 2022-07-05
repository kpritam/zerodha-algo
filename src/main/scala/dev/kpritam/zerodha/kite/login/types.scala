package dev.kpritam.zerodha.kite.login

import zio.json.*

case class LoginResponse(status: String, data: Data)
case class Data(user_id: String, request_id: String, twofa_type: String, twofa_status: String)

object LoginResponse:
  implicit val decoder: JsonDecoder[LoginResponse] = DeriveJsonDecoder.gen[LoginResponse]

object Data:
  implicit val decoder: JsonDecoder[Data] = DeriveJsonDecoder.gen[Data]

case class TwofaResponse(status: String)

object TwofaResponse:
  implicit val decoder: JsonDecoder[TwofaResponse] = DeriveJsonDecoder.gen[TwofaResponse]
