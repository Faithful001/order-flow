package com.king.orderflow.domain.order;

import com.king.orderflow.domain.order.dto.BookSnapshot;
import com.king.orderflow.domain.order.dto.SubmitOrderRequest;
import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.shared.Trade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<List<Trade>> submit(@Valid @RequestBody SubmitOrderRequest request) {
        return ResponseEntity.ok(orderService.submit(request));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID orderId,
            @RequestParam String instrument,
            @RequestParam OrderSide side
    ) {
        boolean cancelled = orderService.cancel(orderId, instrument, side);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{instrument}/book")
    public ResponseEntity<BookSnapshot> getBook(@PathVariable String instrument) {
        return ResponseEntity.ok(orderService.getBook(instrument));
    }

}
