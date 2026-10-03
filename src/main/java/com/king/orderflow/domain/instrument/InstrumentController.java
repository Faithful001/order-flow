package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.dto.CreateInstrumentRequest;
import com.king.orderflow.domain.instrument.dto.InstrumentResponse;
import com.king.orderflow.shared.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping
    public ResponseEntity<Response<List<InstrumentResponse>>> getAllInstruments() {
        return ResponseEntity.ok(Response.success("All instruments", instrumentService.getAllInstruments()));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Response<InstrumentResponse>> getInstrument(@PathVariable String symbol) {
        return ResponseEntity.ok(Response.success(instrumentService.getInstrumentBySymbol(symbol)));
    }

    @PostMapping
    public ResponseEntity<Response<InstrumentResponse>> createInstrument(@Valid @RequestBody CreateInstrumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.success(instrumentService.createInstrument(request)));
    }
}
