package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.dto.CreateInstrumentRequest;
import com.king.orderflow.domain.instrument.dto.InstrumentResponse;
import com.king.orderflow.domain.instrument.enums.InstrumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InstrumentService {

    private final InstrumentEngineRegistry engineRegistry;
    private final ConcurrentHashMap<String, Instrument> instruments = new ConcurrentHashMap<>();

    public InstrumentService(InstrumentEngineRegistry engineRegistry) {
        this.engineRegistry = engineRegistry;
        // Default seed instrument for convenience
        Instrument defaultBtc = Instrument.builder()
                .symbol("BTC-USD")
                .baseCurrency("BTC")
                .quoteCurrency("USD")
                .tickSize(new BigDecimal("0.01"))
                .minQuantity(new BigDecimal("0.001"))
                .status(InstrumentStatus.TRADING)
                .build();
        instruments.put(defaultBtc.getSymbol(), defaultBtc);
        engineRegistry.getOrCreate(defaultBtc.getSymbol());
    }

    public List<InstrumentResponse> getAllInstruments() {
        return instruments.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public InstrumentResponse getInstrument(String symbol) {
        Instrument instrument = instruments.get(symbol);
        if (instrument == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found: " + symbol);
        }
        return toResponse(instrument);
    }

    public InstrumentResponse createInstrument(CreateInstrumentRequest request) {
        if (instruments.containsKey(request.symbol())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists: " + request.symbol());
        }

        Instrument instrument = Instrument.builder()
                .symbol(request.symbol())
                .baseCurrency(request.baseCurrency())
                .quoteCurrency(request.quoteCurrency())
                .tickSize(request.tickSize())
                .minQuantity(request.minQuantity())
                .status(InstrumentStatus.TRADING)
                .build();

        instruments.put(instrument.getSymbol(), instrument);
        engineRegistry.getOrCreate(instrument.getSymbol());

        return toResponse(instrument);
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getSymbol(),
                instrument.getBaseCurrency(),
                instrument.getQuoteCurrency(),
                instrument.getTickSize(),
                instrument.getMinQuantity(),
                instrument.getStatus()
        );
    }
}
