package com.king.orderflow.domain.orderbook;

import com.king.orderflow.domain.order.Order;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.enums.OrderStatus;
import com.king.orderflow.domain.order.enums.OrderType;
import com.king.orderflow.shared.Trade;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class OrderBook {

    private final String instrument;
    private final ReentrantLock lock = new ReentrantLock();

    private final TreeMap<BigDecimal, Deque<Order>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    private final TreeMap<BigDecimal, Deque<Order>> asks =
            new TreeMap<>();

    public OrderBook(String instrument) {
        if (instrument == null || instrument.isBlank()) {
            throw new IllegalArgumentException("instrument must not be null or blank");
        }
        this.instrument = instrument;
    }

    public String getInstrument() {
        return instrument;
    }

    public List<Trade> submit(Order incomingOrder) {
        validate(incomingOrder);

        lock.lock();
        try {
            List<Trade> trades = new ArrayList<>();

            TreeMap<BigDecimal, Deque<Order>> oppositeSide =
                    incomingOrder.getSide() == OrderSide.BUY ? asks : bids;

            while (hasRemaining(incomingOrder)
                    && !oppositeSide.isEmpty()
                    && crosses(incomingOrder, oppositeSide.firstKey())) {

                BigDecimal bestPrice = oppositeSide.firstKey();
                Deque<Order> queueAtBestPrice = oppositeSide.get(bestPrice);
                Order restingOrder = queueAtBestPrice.peekFirst();

                BigDecimal matchedQuantity = incomingOrder.getRemainingQuantity()
                        .min(restingOrder.getRemainingQuantity());

                incomingOrder.setRemainingQuantity(
                        incomingOrder.getRemainingQuantity().subtract(matchedQuantity));
                restingOrder.setRemainingQuantity(
                        restingOrder.getRemainingQuantity().subtract(matchedQuantity));

                trades.add(new Trade(
                        incomingOrder.getId(),
                        restingOrder.getId(),
                        bestPrice,
                        matchedQuantity
                ));

                if (!hasRemaining(restingOrder)) {
                    queueAtBestPrice.pollFirst();
                    restingOrder.setStatus(OrderStatus.FILLED);
                } else {
                    restingOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                }

                if (queueAtBestPrice.isEmpty()) {
                    oppositeSide.remove(bestPrice);
                }
            }

            finalizeIncomingOrder(incomingOrder, trades);

            return trades;
        } finally {
            lock.unlock();
        }
    }

    public boolean cancel(UUID orderId, OrderSide side) {
        lock.lock();
        try {
            TreeMap<BigDecimal, Deque<Order>> map = side == OrderSide.BUY ? bids : asks;

            for (Iterator<Map.Entry<BigDecimal, Deque<Order>>> it =
                 map.entrySet().iterator(); it.hasNext(); ) {

                Map.Entry<BigDecimal, Deque<Order>> entry = it.next();
                Deque<Order> queue = entry.getValue();

                Iterator<Order> queueIt = queue.iterator();
                while (queueIt.hasNext()) {
                    Order order = queueIt.next();
                    if (order.getId().equals(orderId)) {
                        queueIt.remove();
                        order.setStatus(OrderStatus.CANCELLED);
                        if (queue.isEmpty()) {
                            it.remove();
                        }
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal bestBid() {
        lock.lock();
        try {
            return bids.isEmpty() ? null : bids.firstKey();
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal bestAsk() {
        lock.lock();
        try {
            return asks.isEmpty() ? null : asks.firstKey();
        } finally {
            lock.unlock();
        }
    }

    public List<PriceLevel> bidLevels() {
        lock.lock();
        try {
            return snapshot(bids);
        } finally {
            lock.unlock();
        }
    }

    public List<PriceLevel> askLevels() {
        lock.lock();
        try {
            return snapshot(asks);
        } finally {
            lock.unlock();
        }
    }

    private List<PriceLevel> snapshot(TreeMap<BigDecimal, Deque<Order>> side) {
        List<PriceLevel> levels = new ArrayList<>(side.size());
        for (Map.Entry<BigDecimal, Deque<Order>> entry : side.entrySet()) {
            BigDecimal totalQuantity = entry.getValue().stream()
                    .map(Order::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            levels.add(new PriceLevel(entry.getKey(), totalQuantity));
        }
        return levels;
    }

    private void finalizeIncomingOrder(Order incomingOrder, List<Trade> trades) {
        if (hasRemaining(incomingOrder)) {
            if (incomingOrder.getType() == OrderType.LIMIT) {
                incomingOrder.setStatus(
                        trades.isEmpty() ? OrderStatus.OPEN : OrderStatus.PARTIALLY_FILLED);
                restInBook(incomingOrder);
            } else {
                incomingOrder.setStatus(OrderStatus.CANCELLED);
            }
        } else {
            incomingOrder.setStatus(OrderStatus.FILLED);
        }
    }

    private void restInBook(Order order) {
        TreeMap<BigDecimal, Deque<Order>> ownSide =
                order.getSide() == OrderSide.BUY ? bids : asks;

        ownSide.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>())
                .addLast(order);
    }

    private boolean crosses(Order incomingOrder, BigDecimal oppositePrice) {
        if (incomingOrder.getType() == OrderType.MARKET) {
            return true;
        }
        BigDecimal limitPrice = incomingOrder.getPrice();
        if (incomingOrder.getSide() == OrderSide.BUY) {
            // buyer crosses if the best ask is at or below what they'll pay
            return oppositePrice.compareTo(limitPrice) <= 0;
        } else {
            // seller crosses if the best bid is at or above what they'll accept
            return oppositePrice.compareTo(limitPrice) >= 0;
        }
    }

    private boolean hasRemaining(Order order) {
        return order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private void validate(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        if (!instrument.equals(order.getInstrument())) {
            throw new IllegalArgumentException(
                    "order instrument '" + order.getInstrument()
                            + "' does not match this book's instrument '" + instrument + "'");
        }
        if (order.getRemainingQuantity() == null
                || order.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("order remainingQuantity must be positive");
        }
        if (order.getType() == OrderType.LIMIT
                && (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("LIMIT order must have a positive price");
        }
        if (order.getType() == OrderType.MARKET && order.getPrice() != null) {
            throw new IllegalArgumentException("MARKET order must not have a price");
        }
    }
}