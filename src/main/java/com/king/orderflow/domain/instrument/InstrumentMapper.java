package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.dto.CreateInstrumentRequest;
import com.king.orderflow.domain.instrument.dto.InstrumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InstrumentMapper {

    InstrumentResponse toResponse(Instrument instrument);

    List<InstrumentResponse> toResponses(List<Instrument> instruments);

    @Mapping(target = "status", constant = "TRADING")
    Instrument toEntity(CreateInstrumentRequest request);
}