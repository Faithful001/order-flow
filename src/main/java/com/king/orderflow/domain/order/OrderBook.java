package com.king.orderflow.domain.order;

import com.king.orderflow.domain.order.dto.PriceLevel;
import com.king.orderflow.domain.order.dto.Trade;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.enums.OrderStatus;
import com.king.orderflow.domain.order.enums.OrderType;
import com.king.orderflow.shared.exception.InvalidOrderException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

public class OrderBook {

    @Getter
    private final String instrument;

    private final TreeMap<BigDecimal, Deque<Order>> bids = new TreeMap<>(Comparator.reverseOrder()); // price -> Deque<Order>
    private final TreeMap<BigDecimal, Deque<Order>> asks = new TreeMap<>(); // price -> Deque<Order>

    private final Map<UUID, Order> restingById = new HashMap<>(); // orderId -> Order

    public OrderBook(String instrument) {
        if (instrument == null || instrument.isBlank()) {
            throw new IllegalArgumentException("instrument must not be null or blank");
        }
        this.instrument = instrument;
    }

    public List<Trade> submit(Order incoming) {
        validate(incoming);
        List<Trade> trades = match(incoming);
        settle(incoming, !trades.isEmpty());
        return trades;
    }

    public boolean cancel(UUID orderId) {
        Order order = restingById.remove(orderId);
        if (order == null) {
            return false;
        }
        TreeMap<BigDecimal, Deque<Order>> side = sideOf(order);
        Deque<Order> queue = side.get(order.getPrice());
        queue.removeIf(o -> o.getId().equals(orderId));
        if (queue.isEmpty()) {
            side.remove(order.getPrice());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return true;
    }

    public BigDecimal bestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    public BigDecimal bestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    public List<PriceLevel> bidLevels() {
        return levelsOf(bids);
    }

    public List<PriceLevel> askLevels() {
        return levelsOf(asks);
    }


    private List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();
        TreeMap<BigDecimal, Deque<Order>> opposite = oppositeSideOf(incoming);

        while (hasRemaining(incoming) && !opposite.isEmpty()) {
            Map.Entry<BigDecimal, Deque<Order>> best = opposite.firstEntry();
            BigDecimal price = best.getKey();
            if (!crosses(incoming, price)) {
                break;
            }

            Deque<Order> queue = best.getValue();
            Order resting = queue.peekFirst();

            BigDecimal quantity = incoming.getRemainingQuantity().min(resting.getRemainingQuantity());
            incoming.setRemainingQuantity(incoming.getRemainingQuantity().subtract(quantity));
            resting.setRemainingQuantity(resting.getRemainingQuantity().subtract(quantity));

            trades.add(new Trade(incoming.getId(), resting.getId(), price, quantity));

            if (hasRemaining(resting)) {
                resting.setStatus(OrderStatus.PARTIALLY_FILLED);
            } else {
                queue.pollFirst();
                restingById.remove(resting.getId());
                resting.setStatus(OrderStatus.FILLED);
            }

            if (queue.isEmpty()) {
                opposite.remove(price);
            }
        }
        return trades;
    }

//     Decides the incoming order's final status and rests any unfilled limit remainder.
    private void settle(Order incoming, boolean traded) {
        if (!hasRemaining(incoming)) {
            incoming.setStatus(OrderStatus.FILLED);
        } else if (incoming.getType() == OrderType.LIMIT) {
            incoming.setStatus(traded ? OrderStatus.PARTIALLY_FILLED : OrderStatus.OPEN);
            sideOf(incoming).computeIfAbsent(incoming.getPrice(), p -> new ArrayDeque<>())
                    .addLast(incoming);
            restingById.put(incoming.getId(), incoming);
        } else {
            incoming.setStatus(OrderStatus.CANCELLED);   // unfilled market remainder
        }
    }

    private boolean crosses(Order incoming, BigDecimal oppositePrice) {
        if (incoming.getType() == OrderType.MARKET) {
            return true;
        }
        int cmp = oppositePrice.compareTo(incoming.getPrice());
        return incoming.getSide() == OrderSide.BUY ? cmp <= 0 : cmp >= 0;
    }

    private TreeMap<BigDecimal, Deque<Order>> sideOf(Order order) {
        return order.getSide() == OrderSide.BUY ? bids : asks;
    }

    private TreeMap<BigDecimal, Deque<Order>> oppositeSideOf(Order order) {
        return order.getSide() == OrderSide.BUY ? asks : bids;
    }

    private boolean hasRemaining(Order order) {
        return order.getRemainingQuantity().signum() > 0;
    }

    private List<PriceLevel> levelsOf(Map<BigDecimal, Deque<Order>> side) {
        return side.entrySet().stream()
                .map(e -> new PriceLevel(e.getKey(), totalRemaining(e.getValue())))
                .toList();   // immutable
    }

    private BigDecimal totalRemaining(Deque<Order> queue) {
        return queue.stream()
                .map(Order::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validate(Order order) {
        if (order == null) {
            throw new InvalidOrderException("order must not be null");
        }
        if (order.getId() == null) {
            throw new InvalidOrderException("order id must not be null");
        }
        if (order.getSide() == null) {
            throw new InvalidOrderException("order side must not be null");
        }
        if (order.getType() == null) {
            throw new InvalidOrderException("order type must not be null");
        }
        if (!instrument.equals(order.getInstrument())) {
            throw new InvalidOrderException("order instrument '" + order.getInstrument()
                    + "' does not match this book's instrument '" + instrument + "'");
        }
        if (order.getRemainingQuantity() == null || order.getRemainingQuantity().signum() <= 0) {
            throw new InvalidOrderException("order remainingQuantity must be positive");
        }
        if (order.getType() == OrderType.LIMIT
                && (order.getPrice() == null || order.getPrice().signum() <= 0)) {
            throw new InvalidOrderException("LIMIT order must have a positive price");
        }
        if (order.getType() == OrderType.MARKET && order.getPrice() != null) {
            throw new InvalidOrderException("MARKET order must not have a price");
        }
        if (restingById.containsKey(order.getId())) {
            throw new InvalidOrderException("order " + order.getId() + " is already resting in the book");
        }
    }
}