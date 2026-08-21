package com.king.orderflow.domain.order.enums;

public enum OrderType {
    // Has a price limit. Buys will not pay more than this price,
    // sells will not accept less. Unfilled quantity rests in the book.
    LIMIT,

    // No price limit. Executes against whatever is available,
    // walking through price levels until filled or the book runs out.
    // Decide explicitly (per the earlier discussion) what happens to
    // any unfilled remainder: MARKET orders in this project are
    // treated as IOC by default, see OrderStatus.CANCELLED below,
    // rather than resting as a limit order at the last traded price.
    // State this decision in your README rather than leaving it
    // implicit in the code.
    MARKET
}