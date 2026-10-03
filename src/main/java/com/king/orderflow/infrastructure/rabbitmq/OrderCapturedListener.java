package com.king.orderflow.infrastructure.rabbitmq;

import com.king.orderflow.domain.order.OrderService;
import com.king.orderflow.domain.order.message.OrderCapturedEvent;
import com.king.orderflow.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCapturedListener {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void onOrderCaptured(OrderCapturedEvent event) {
        try {
            orderService.process(event);
        } catch (DomainException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}