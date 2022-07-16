package dev.kpritam.zerodha.kite.models

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import com.zerodhatech.kiteconnect.kitehttp.exceptions.TokenException

enum KiteError(msg: String, cause: Throwable = null) extends Throwable(msg, cause):
  case InvalidInstrumentToken(token: String)                extends KiteError(s"Invalid instrument token: $token")
  case QuoteNoteFound(instrument: String)                   extends KiteError(s"Quote note found for $instrument")
  case Error(msg: String, cause: Throwable = null)          extends KiteError(msg, cause)
  case SessionExpired(msg: String, cause: Throwable = null) extends KiteError(msg, cause)
  case LoginFailed(msg: String, cause: Throwable)           extends KiteError(msg, cause)

object KiteError:
  def apply(e: Throwable): KiteError =
    e match
      case se: TokenException => KiteError.SessionExpired(se.message, se)
      case ke: KiteException  => KiteError.Error(ke.message, ke)
      case _                  => KiteError.Error(e.getMessage, e)
