package com.king.orderflow.domain.order;

import com.king.orderflow.domain.instrument.Instrument;
import com.king.orderflow.domain.instrument.InstrumentEngine;
import com.king.orderflow.domain.instrument.InstrumentRepository;
import com.king.orderflow.domain.instrument.enums.InstrumentStatus;
import com.king.orderflow.domain.order.dto.BookSnapshot;
import com.king.orderflow.domain.order.dto.SubmitOrderRequest;
import com.king.orderflow.domain.order.enums.OrderStatus;
import com.king.orderflow.domain.order.message.OrderCapturedEvent;
import com.king.orderflow.infrastructure.rabbitmq.OrderEventPublisher;
import com.king.orderflow.infrastructure.websocket.BookUpdatePublisher;
import com.king.orderflow.shared.exception.InstrumentNotTradingException;
import com.king.orderflow.shared.exception.UnknownInstrumentException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final InstrumentRepository instrumentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final BookUpdatePublisher bookUpdatePublisher;

    private final ConcurrentHashMap<String, InstrumentEngine> engines = new ConcurrentHashMap<>();

    public UUID submit(SubmitOrderRequest request) {
        Instrument instrument = requireInstrument(request.instrument());
        if (instrument.getStatus() != InstrumentStatus.TRADING) {
            throw new InstrumentNotTradingException(request.instrument());
        }

        UUID orderId = UUID.randomUUID();
        orderEventPublisher.publish(
                new OrderCapturedEvent(
                        orderId,
                        request.instrument(),
                        request.side(),
                        request.type(),
                        request.price(),
                        request.quantity()
                )
        );
        return orderId;
    }

    public CompletableFuture<Boolean> cancel(UUID orderId, String instrument) {
        requireInstrument(instrument);
        InstrumentEngine engine = engineFor(instrument);
        return engine.cancel(orderId)
                .thenApply(cancelled -> {
                    if (cancelled) {
                        orderRepository.findById(orderId)
                                .ifPresent(order -> {
                                    order.setStatus(OrderStatus.CANCELLED);
                                    orderRepository.save(order);
                                });
                        bookUpdatePublisher.publish(engine.snapshot());
                    }
                    return cancelled;
                });
    }

    public BookSnapshot getBook(String instrument) {
        requireInstrument(instrument);
        return engineFor(instrument).snapshot();
    }

    public void process(OrderCapturedEvent event) {
        Order order = Order.builder()
                .id(event.orderId())
                .instrument(event.instrument())
                .side(event.side())
                .type(event.type())
                .price(event.price())
                .quantity(event.quantity())
                .remainingQuantity(event.quantity())
                .status(OrderStatus.OPEN)
                .build();

        orderRepository.save(order);

        InstrumentEngine engine = engineFor(event.instrument());
        await(engine.submit(order));
        orderRepository.save(order);
        bookUpdatePublisher.publish(engine.snapshot());
    }

    private Instrument requireInstrument(String symbol) {
        return instrumentRepository.findBySymbol(symbol)
                .orElseThrow(() -> new UnknownInstrumentException(symbol + " instrument not found"));
    }

    private InstrumentEngine engineFor(String symbol) {
        return engines.computeIfAbsent(symbol, InstrumentEngine::new);
    }

    private <T> void await(CompletableFuture<T> future) {
        try {
            future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    @PreDestroy
    void shutdown() {
        engines.values().forEach(InstrumentEngine::shutdown);
    }
}