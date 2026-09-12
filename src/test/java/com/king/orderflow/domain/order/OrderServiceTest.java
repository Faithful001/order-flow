package com.king.orderflow.domain.order;

import com.king.orderflow.domain.instrument.InstrumentEngineRegistry;
import com.king.orderflow.infrastructure.websocket.BookUpdatePublisher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {
    @Mock
    private InstrumentEngineRegistry registry;
    @Mock
    private BookUpdatePublisher bookUpdatePublisher;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("Submit Order Tests")
    class SubmitOrderTests {
        @Test
        @DisplayName("Should submit order successfully ")
        void test() {

        }

    }



}