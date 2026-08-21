package com.king.orderflow.domain.order.enums;

public enum OrderStatus {
    // Sitting in the book, not yet matched at all, or the matching
    // loop hasn't touched it yet.
    OPEN,

    // Matched against one or more resting orders, but remainingQuantity
    // is still greater than zero. Still sits in the book at its price
    // level, per Case 3 from the walkthrough (Order A keeping its
    // leftover 6 shares).
    PARTIALLY_FILLED,

    // remainingQuantity has reached zero. Fully matched, removed from
    // the book. Terminal state.
    FILLED,

    // Removed from the book before being fully filled, either by
    // explicit user cancellation, or automatically for a MARKET order
    // that couldn't find enough liquidity to fill completely (the
    // policy decision from OrderType.MARKET above). Terminal state.
    CANCELLED
}