package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.dto.CreateInstrumentRequest;
import com.king.orderflow.domain.instrument.dto.InstrumentResponse;
import com.king.orderflow.domain.instrument.enums.InstrumentStatus;
import com.king.orderflow.shared.exception.InstrumentAlreadyExistsException;
import com.king.orderflow.shared.exception.UnknownInstrumentException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@AllArgsConstructor
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;
    private final InstrumentMapper instrumentMapper;

    @Transactional(readOnly = true)
    public List<InstrumentResponse> getAllInstruments() {
        return instrumentMapper.toResponses(instrumentRepository.findAll());
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrumentBySymbol(String symbol) {
        return instrumentRepository.findBySymbol(symbol)
                .map(instrumentMapper::toResponse)
                .orElseThrow(() -> new UnknownInstrumentException(symbol + " instrument not found"));
    }

    @Transactional
    public InstrumentResponse createInstrument(CreateInstrumentRequest request) {
        if (instrumentRepository.existsBySymbol(request.symbol())) {
            throw new InstrumentAlreadyExistsException(request.symbol());
        }
        Instrument saved = instrumentRepository.save(instrumentMapper.toEntity(request));
        return instrumentMapper.toResponse(saved);
    }
}