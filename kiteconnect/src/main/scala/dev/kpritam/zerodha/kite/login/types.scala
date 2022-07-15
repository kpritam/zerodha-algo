package dev.kpritam.zerodha.kite.login

import zio.json.*

case class LoginResponse(status: String, data: Data)
case class Data(user_id: String, request_id: String, twofa_type: String, twofa_status: String)

object LoginResponse:
  given JsonDecoder[LoginResponse] = DeriveJsonDecoder.gen[LoginResponse]

object Data:
  given JsonDecoder[Data] = DeriveJsonDecoder.gen[Data]

case class TwofaResponse(status: String)

object TwofaResponse:
  given JsonDecoder[TwofaResponse] = DeriveJsonDecoder.gen[TwofaResponse]
