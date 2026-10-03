package com.king.orderflow.domain.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    public Optional<Instrument> findBySymbol(String symbol);
    public Boolean existsBySymbol(String symbol);
}
