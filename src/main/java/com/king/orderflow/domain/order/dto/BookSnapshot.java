package com.king.orderflow.domain.order.dto;

import java.util.List;

public record BookSnapshot(String instrument, List<PriceLevel> bids, List<PriceLevel> asks) {
}