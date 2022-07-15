package dev.kpritam.zerodha.utils

import com.zerodhatech.models.Quote

import scala.math.{min, round}

def triggerPrice(price: Double) =
  round(price * 2.5 / 0.05) * 0.05

def triggerPriceAndPrice(price: Double, quote: Quote): (Double, Double) =
  val tp = triggerPrice(price)
  (tp, min(tp + 0.1, quote.upperCircuitLimit))
