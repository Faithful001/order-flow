package com.king.orderflow.domain.orderbook;

import com.king.orderflow.domain.orderbook.dto.PriceLevel;
import com.king.orderflow.domain.orderbook.dto.SubmitOrderRequest;
import com.king.orderflow.domain.orderbook.enums.OrderSide;
import com.king.orderflow.domain.orderbook.enums.Trade;
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

    public record BookSnapshot(List<PriceLevel> bids, List<PriceLevel> asks) {}
}