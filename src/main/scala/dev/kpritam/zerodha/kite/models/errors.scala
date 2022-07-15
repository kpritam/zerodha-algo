package dev.kpritam.zerodha.kite.models

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException

case class LastPriceExceeds(instrumentLastPrice: Double, previousPrice: Double) extends Throwable

enum KiteError(msg: String, cause: Throwable = null) extends Throwable(msg, cause):
  case InvalidInstrumentToken(token: String)       extends KiteError(s"Invalid instrument token: $token")
  case Error(msg: String, cause: Throwable = null) extends KiteError(msg, cause)
  case QuoteNoteFound(instrument: String)          extends KiteError(s"Quote note found for $instrument")

object KiteError:
  def apply(e: Throwable): KiteError.Error =
    e match
      case ke: KiteException => KiteError.Error(ke.message, ke)
      case _                 => KiteError.Error(e.getMessage, e)
