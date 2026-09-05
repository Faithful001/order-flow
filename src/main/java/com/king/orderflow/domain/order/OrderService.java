package com.king.orderflow.domain.order;

import com.king.orderflow.domain.instrument.InstrumentEngine;
import com.king.orderflow.domain.instrument.InstrumentEngineRegistry;
import com.king.orderflow.domain.order.dto.SubmitOrderRequest;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.enums.OrderStatus;
import com.king.orderflow.domain.orderbook.OrderBook;
import com.king.orderflow.shared.Trade;
import com.king.orderflow.infrastructure.websocket.BookUpdatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final InstrumentEngineRegistry registry;
    private final BookUpdatePublisher bookUpdatePublisher;

    public List<Trade> submit(SubmitOrderRequest request) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .instrument(request.instrument())
                .side(request.side())
                .type(request.type())
                .price(request.price())
                .quantity(request.quantity())
                .remainingQuantity(request.quantity())
                .status(OrderStatus.OPEN)
                .build();

        InstrumentEngine engine = registry.getOrCreate(request.instrument());

        try {
            List<Trade> trades = engine.submit(order).get();
            bookUpdatePublisher.publish(engine.getOrderBook());
            return trades;
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while processing order");
        }
    }

    public boolean cancel(UUID orderId, String instrument, OrderSide side) {
        InstrumentEngine engine = resolveEngine(instrument);

        try {
            boolean cancelled = engine.cancel(orderId, side).get();
            if (cancelled) {
                bookUpdatePublisher.publish(engine.getOrderBook());
            }
            return cancelled;
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while cancelling order");
        }
    }

    public OrderController.BookSnapshot getBook(String instrument) {
        InstrumentEngine engine = resolveEngine(instrument);
        OrderBook book = engine.getOrderBook();
        return new OrderController.BookSnapshot(book.bidLevels(), book.askLevels());
    }

    private InstrumentEngine resolveEngine(String instrument) {
        try {
            return registry.get(instrument);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
