package dev.kpritam.zerodha.kite.models

import com.zerodhatech.models.OrderParams

case class OrderRequest(
    exchange: String | Null = null,
    tradingSymbol: String | Null = null,
    transactionType: String | Null = null,
    quantity: Int = 0,
    price: Double = 0,
    product: String | Null = null,
    orderType: String | Null = null,
    validity: String | Null = null,
    disclosedQuantity: Int = 0,
    triggerPrice: Double = 0,
    // fixme: figure out better way
    squareOff: java.lang.Double | Null = null,
    stopLoss: java.lang.Double | Null = null,
    trailingStopLoss: java.lang.Double | Null = null,
    tag: String | Null = null,
    parentOrderId: String | Null = null,
    validityTTL: Int = 0,
    icebergQuantity: Int = 0,
    icebergLegs: Int = 0
):
  def toZerodha: OrderParams =
    val orderParams = OrderParams()
    orderParams.exchange = exchange
    orderParams.tradingsymbol = tradingSymbol
    orderParams.transactionType = transactionType
    orderParams.quantity = quantity
    orderParams.price = price
    orderParams.product = product
    orderParams.orderType = orderType
    orderParams.validity = validity
    orderParams.disclosedQuantity = disclosedQuantity
    orderParams.triggerPrice = triggerPrice
    orderParams.squareoff = squareOff
    orderParams.stoploss = stopLoss
    orderParams.trailingStoploss = trailingStopLoss
    orderParams.tag = tag
    orderParams.parentOrderId = parentOrderId
    orderParams.validityTTL = validityTTL
    orderParams.icebergQuantity = icebergQuantity
    orderParams.icebergLegs = icebergLegs
    orderParams

extension (req: OrderRequest)
  def toSLBuy(triggerPrice: Double, price: Double, tradingSymbol: String): OrderRequest =
    req.copy(
      orderType = "SL",
      transactionType = "BUY",
      triggerPrice = triggerPrice,
      price = price,
      tradingSymbol = tradingSymbol
    )
