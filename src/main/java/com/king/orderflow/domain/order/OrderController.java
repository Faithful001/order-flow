package com.king.orderflow.domain.order;

import com.king.orderflow.domain.order.dto.BookSnapshot;
import com.king.orderflow.domain.order.dto.SubmitOrderRequest;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.dto.Trade;
import com.king.orderflow.shared.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Response<Map<String, UUID>>> submit(@Valid @RequestBody SubmitOrderRequest request) {
        UUID orderId = orderService.submit(request);
        return ResponseEntity.accepted().body(Response.success("Order queued", Map.of("orderId", orderId)));
    }

    @DeleteMapping("/{orderId}")
    public CompletableFuture<ResponseEntity<Void>> cancel(
            @PathVariable UUID orderId,
            @RequestParam String instrument
    ) {
        return orderService.cancel(orderId, instrument)
                .thenApply(cancelled -> cancelled
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }

    @GetMapping("/{instrument}/book")
    public ResponseEntity<BookSnapshot> getBook(@PathVariable String instrument) {
        return ResponseEntity.ok(orderService.getBook(instrument));
    }

}
