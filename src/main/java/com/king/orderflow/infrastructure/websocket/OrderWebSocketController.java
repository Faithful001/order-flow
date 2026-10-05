package com.king.orderflow.infrastructure.websocket;

import com.king.orderflow.domain.order.OrderService;
import com.king.orderflow.domain.order.dto.SubmitOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OrderWebSocketController {

    private final OrderService orderService;
    @MessageMapping("/orders/submit")
    public UUID submit(@Payload SubmitOrderRequest request) {
        return orderService.submit(request);
    }

    @MessageMapping("/orders/{orderId}/cancel/{instrument}")
    public void cancel(
            @DestinationVariable UUID orderId,
            @DestinationVariable String instrument
    ) {
        orderService.cancel(orderId, instrument);
    }
}
