package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.order.Order;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.orderbook.OrderBook;
import com.king.orderflow.shared.Trade;

import java.util.List;
import java.util.concurrent.*;

public class InstrumentEngine {

    private final OrderBook orderBook;
    private final BlockingQueue<QueuedTask> incomingOrders = new LinkedBlockingQueue<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public InstrumentEngine(String instrument) {
        this.orderBook = new OrderBook(instrument);
        worker.submit(this::processLoop);
    }

    public CompletableFuture<List<Trade>> submit(Order order) {
        CompletableFuture<List<Trade>> future = new CompletableFuture<>();
        incomingOrders.offer(new SubmittedOrder(order, future));
        return future;
    }

    public CompletableFuture<Boolean> cancel(java.util.UUID orderId, OrderSide side) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        incomingOrders.offer(new SubmittedCancel(orderId, side, future));
        return future;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                QueuedTask task = incomingOrders.take();
                if (task instanceof SubmittedOrder submitted) {
                    try {
                        List<Trade> trades = orderBook.submit(submitted.order());
                        submitted.future().complete(trades);
                    } catch (Exception e) {
                        submitted.future().completeExceptionally(e);
                    }
                } else if (task instanceof SubmittedCancel cancel) {
                    try {
                        boolean result = orderBook.cancel(cancel.orderId(), cancel.side());
                        cancel.future().complete(result);
                    } catch (Exception e) {
                        cancel.future().completeExceptionally(e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    private interface QueuedTask {}

    private record SubmittedOrder(Order order, CompletableFuture<List<Trade>> future) implements QueuedTask {}

    private record SubmittedCancel(java.util.UUID orderId, OrderSide side, CompletableFuture<Boolean> future) implements QueuedTask {}
}
