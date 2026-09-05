package com.king.orderflow.domain.instrument;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InstrumentEngineRegistry {

    private final ConcurrentHashMap<String, InstrumentEngine> engines = new ConcurrentHashMap<>();

    public InstrumentEngine getOrCreate(String instrument) {
        return engines.computeIfAbsent(instrument, InstrumentEngine::new);
    }

    public InstrumentEngine get(String instrument) {
        InstrumentEngine engine = engines.get(instrument);
        if (engine == null) {
            throw new IllegalArgumentException("No order book exists for instrument: " + instrument);
        }
        return engine;
    }
}
