package com.king.orderflow.domain.order.dto;

import java.util.List;

public record BookSnapshot(List<PriceLevel> bids, List<PriceLevel> asks) {
}
