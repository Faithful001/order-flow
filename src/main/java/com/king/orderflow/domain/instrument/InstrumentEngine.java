package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.order.Order;
import com.king.orderflow.domain.order.dto.BookSnapshot;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.OrderBook;
import com.king.orderflow.domain.order.dto.Trade;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class InstrumentEngine {
    private final String instrument;
    private final OrderBook orderBook;
    private final ExecutorService worker;
    private volatile BookSnapshot snapshot;

    public InstrumentEngine(String instrument) {
        this.instrument = instrument;
        this.orderBook = new OrderBook(instrument);
        this.worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "engine-" + instrument));
        this.snapshot = takeSnapshot();
    }

    public CompletableFuture<List<Trade>> submit(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            List<Trade> trades = orderBook.submit(order);
            snapshot = takeSnapshot();
            return trades;
        }, worker);
    }

    public CompletableFuture<Boolean> cancel(UUID orderId) {
        return CompletableFuture.supplyAsync(() -> {
            boolean cancelled = orderBook.cancel(orderId);
            if (cancelled) {
                snapshot = takeSnapshot();
            }
            return cancelled;
        }, worker);
    }

    public BookSnapshot snapshot() {
        return snapshot;
    }

    public void shutdown() {
        worker.shutdown();
    }

    private BookSnapshot takeSnapshot() {
        return new BookSnapshot(instrument, orderBook.bidLevels(), orderBook.askLevels());
    }
}