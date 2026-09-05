package com.king.orderflow.infrastructure.websocket;

import com.king.orderflow.domain.orderbook.OrderBook;
import com.king.orderflow.domain.orderbook.PriceLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(OrderBook book) {
        BookSnapshot snapshot = new BookSnapshot(book.bidLevels(), book.askLevels());
        messagingTemplate.convertAndSend("/topic/book/" + book.getInstrument(), snapshot);
    }

    public record BookSnapshot(
            java.util.List<PriceLevel> bids,
            java.util.List<PriceLevel> asks
    ) {}
}