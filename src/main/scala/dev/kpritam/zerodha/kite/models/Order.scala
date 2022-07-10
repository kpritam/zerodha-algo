package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.{Quote, Order as ZOrder}

import java.util.{Date, Map}
import scala.math.round

case class Order(
    exchangeOrderId: String | Null = null,
    disclosedQuantity: Double = 0,
    validity: String | Null = null,
    tradingSymbol: String | Null = null,
    orderVariety: String | Null = null,
    orderType: String | Null = null,
    triggerPrice: Double = 0,
    statusMessage: String | Null = null,
    price: Double = 0,
    status: String | Null = null,
    product: String | Null = null,
    accountId: String | Null = null,
    exchange: String | Null = null,
    orderId: String = "",
    pendingQuantity: String | Null = null,
    orderTimestamp: Date | Null = null,
    exchangeTimestamp: Date | Null = null,
    exchangeUpdateTimestamp: Date | Null = null,
    avgPrice: Double = 0,
    transactionType: String | Null = null,
    filledQuantity: String | Null = null,
    quantity: Double = 0,
    parentOrderId: String | Null = null,
    tag: String | Null = null,
    guid: String | Null = null,
    validityTtl: Int = 0
):
  def completed: Boolean = status != null && status.equalsIgnoreCase("COMPLETE")

object Order:
  def from(o: ZOrder): Order =
    Order(
      o.exchangeOrderId,
      o.disclosedQuantity.toDoubleOption.getOrElse(0),
      o.validity,
      o.tradingSymbol,
      o.orderVariety,
      o.orderType,
      o.triggerPrice.toDoubleOption.getOrElse(0),
      o.statusMessage,
      o.price.toDoubleOption.getOrElse(0),
      o.status,
      o.product,
      o.accountId,
      o.exchange,
      o.orderId,
      o.pendingQuantity,
      o.orderTimestamp,
      o.exchangeTimestamp,
      o.exchangeUpdateTimestamp,
      o.averagePrice.toDoubleOption.getOrElse(0),
      o.transactionType,
      o.filledQuantity,
      o.quantity.toDoubleOption.getOrElse(0),
      o.parentOrderId,
      o.tag,
      o.guid,
      o.validityTTL
    )

extension (o: ZOrder) def toOrder = Order.from(o)
