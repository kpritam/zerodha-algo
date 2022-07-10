package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.{Quote, Order as ZOrder}

import java.util.{Date, Map}
import scala.math.round

case class Order(
    exchangeOrderId: String | Null = null,
    disclosedQuantity: Double | Null = null,
    validity: String | Null = null,
    tradingSymbol: String | Null = null,
    orderVariety: String | Null = null,
    orderType: String | Null = null,
    triggerPrice: Double | Null = null,
    statusMessage: String | Null = null,
    price: Double | Null = null,
    status: String | Null = null,
    product: String | Null = null,
    accountId: String | Null = null,
    exchange: String | Null = null,
    orderId: String | Null = null,
    pendingQuantity: String | Null = null,
    orderTimestamp: Date | Null = null,
    exchangeTimestamp: Date | Null = null,
    exchangeUpdateTimestamp: Date | Null = null,
    avgPrice: Double | Null = null,
    transactionType: String | Null = null,
    filledQuantity: String | Null = null,
    quantity: Double | Null = null,
    parentOrderId: String | Null = null,
    tag: String | Null = null,
    guid: String | Null = null,
    validityTTL: Int | Null = null
):
  def completed: Boolean = status != null && status.equalsIgnoreCase("COMPLETE")

  // fixme: why flow typing does not work?
  def avgPriceOrZero: Double = if avgPrice != null then avgPrice.nn else 0
  def priceOrZero: Double    = if price != null then price.nn else 0

object Order:
  def from(o: ZOrder): Order =
    Order(
      o.exchangeOrderId,
      o.disclosedQuantity.toDoubleOption.orNull,
      o.validity,
      o.tradingSymbol,
      o.orderVariety,
      o.orderType,
      o.triggerPrice.toDoubleOption.orNull,
      o.statusMessage,
      o.price.toDoubleOption.orNull,
      o.status,
      o.product,
      o.accountId,
      o.exchange,
      o.orderId,
      o.pendingQuantity,
      o.orderTimestamp,
      o.exchangeTimestamp,
      o.exchangeUpdateTimestamp,
      o.averagePrice.toDoubleOption.orNull,
      o.transactionType,
      o.filledQuantity,
      o.quantity.toDoubleOption.orNull,
      o.parentOrderId,
      o.tag,
      o.guid,
      o.validityTTL
    )

extension (o: ZOrder) def toOrder = Order.from(o)
