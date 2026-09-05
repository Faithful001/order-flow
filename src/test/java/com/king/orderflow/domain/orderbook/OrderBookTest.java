package com.king.orderflow.domain.orderbook;

import com.king.orderflow.domain.order.Order;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.enums.OrderStatus;
import com.king.orderflow.domain.order.enums.OrderType;
import com.king.orderflow.shared.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook("TEST");
    }

    private Order limitOrder(OrderSide side, BigDecimal price, BigDecimal quantity) {
        return Order.builder()
                .id(UUID.randomUUID())
                .instrument("TEST")
                .side(side)
                .type(OrderType.LIMIT)
                .price(price)
                .quantity(quantity)
                .remainingQuantity(quantity)
                .status(OrderStatus.OPEN)
                .build();
    }

    @Test
    void fullyFillsAgainstOneRestingOrder() {
        // resting: sell 10 @ 101
        Order restingSell = limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("10"));
        book.submit(restingSell);

        // incoming: buy 10 @ 101, should fully consume the resting sell
        Order incomingBuy = limitOrder(OrderSide.BUY, new BigDecimal("101"), new BigDecimal("10"));
        List<Trade> trades = book.submit(incomingBuy);

        assertEquals(1, trades.size());
        assertEquals(new BigDecimal("10"), trades.get(0).quantity());
        assertEquals(new BigDecimal("101"), trades.get(0).price());

        assertEquals(OrderStatus.FILLED, incomingBuy.getStatus());
        assertEquals(OrderStatus.FILLED, restingSell.getStatus());
        assertNull(book.bestAsk(), "resting sell should be fully removed from the book");
    }

    @Test
    void fullyFillsAcrossMultipleOrdersAtSamePriceLevel() {
        // resting: two separate sell orders at the same price, 101
        Order sell1 = limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("6"));
        Order sell2 = limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("8"));
        book.submit(sell1);
        book.submit(sell2);

        // incoming: buy 10 @ 101, should consume all of sell1 (6) then
        // part of sell2 (4)
        Order incomingBuy = limitOrder(OrderSide.BUY, new BigDecimal("101"), new BigDecimal("10"));
        List<Trade> trades = book.submit(incomingBuy);

        assertEquals(2, trades.size(), "should produce two separate trades");
        assertEquals(new BigDecimal("6"), trades.get(0).quantity());
        assertEquals(sell1.getId(), trades.get(0).restingOrderId());
        assertEquals(new BigDecimal("4"), trades.get(1).quantity());
        assertEquals(sell2.getId(), trades.get(1).restingOrderId());

        assertEquals(OrderStatus.FILLED, incomingBuy.getStatus());
        assertEquals(OrderStatus.FILLED, sell1.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sell2.getStatus());
        assertEquals(new BigDecimal("4"), sell2.getRemainingQuantity());
    }

    @Test
    void partiallyFillsAndRestingRemainderStaysInBook() {
        // resting: sell 10 @ 101
        Order restingSell = limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("10"));
        book.submit(restingSell);

        // incoming: buy only 4 @ 101
        Order incomingBuy = limitOrder(OrderSide.BUY, new BigDecimal("101"), new BigDecimal("4"));
        List<Trade> trades = book.submit(incomingBuy);

        assertEquals(1, trades.size());
        assertEquals(new BigDecimal("4"), trades.get(0).quantity());

        assertEquals(OrderStatus.FILLED, incomingBuy.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, restingSell.getStatus());
        assertEquals(new BigDecimal("6"), restingSell.getRemainingQuantity());
        assertEquals(new BigDecimal("101"), book.bestAsk(), "resting sell should still be in the book");
    }

    @Test
    void doesNotCrossAndRestsEntirely() {
        // resting: sell 10 @ 101
        book.submit(limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("10")));

        // incoming: buy 5 @ 97, does not cross 101
        Order incomingBuy = limitOrder(OrderSide.BUY, new BigDecimal("97"), new BigDecimal("5"));
        List<Trade> trades = book.submit(incomingBuy);

        assertTrue(trades.isEmpty(), "no trade should occur");
        assertEquals(OrderStatus.OPEN, incomingBuy.getStatus());
        assertEquals(new BigDecimal("97"), book.bestBid());
        assertEquals(new BigDecimal("101"), book.bestAsk(), "resting sell should be untouched");
    }

    @Test
    void crossesMultiplePriceLevels() {
        // resting book: 101 (10), 102 (5), 103 (20)
        Order sellAt101 = limitOrder(OrderSide.SELL, new BigDecimal("101"), new BigDecimal("10"));
        Order sellAt102 = limitOrder(OrderSide.SELL, new BigDecimal("102"), new BigDecimal("5"));
        Order sellAt103 = limitOrder(OrderSide.SELL, new BigDecimal("103"), new BigDecimal("20"));
        book.submit(sellAt101);
        book.submit(sellAt102);
        book.submit(sellAt103);

        // incoming: buy 30 @ 103, should walk through all three levels
        Order incomingBuy = limitOrder(OrderSide.BUY, new BigDecimal("103"), new BigDecimal("30"));
        List<Trade> trades = book.submit(incomingBuy);

        assertEquals(3, trades.size(), "should walk through all three price levels");

        assertEquals(new BigDecimal("101"), trades.get(0).price());
        assertEquals(new BigDecimal("10"), trades.get(0).quantity());

        assertEquals(new BigDecimal("102"), trades.get(1).price());
        assertEquals(new BigDecimal("5"), trades.get(1).quantity());

        assertEquals(new BigDecimal("103"), trades.get(2).price());
        assertEquals(new BigDecimal("15"), trades.get(2).quantity());

        assertEquals(OrderStatus.FILLED, incomingBuy.getStatus());
        assertEquals(OrderStatus.FILLED, sellAt101.getStatus());
        assertEquals(OrderStatus.FILLED, sellAt102.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sellAt103.getStatus());
        assertEquals(new BigDecimal("5"), sellAt103.getRemainingQuantity());

        assertEquals(new BigDecimal("103"), book.bestAsk(), "level 103 should still have 5 remaining");
    }
}