package com.king.orderflow.infrastructure.websocket;

import com.king.orderflow.domain.order.OrderBook;
import com.king.orderflow.domain.order.dto.BookSnapshot;
import com.king.orderflow.domain.order.dto.PriceLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(BookSnapshot snapshot) {
        messagingTemplate.convertAndSend("/topic/book/" + snapshot.instrument(), snapshot);
    }
}