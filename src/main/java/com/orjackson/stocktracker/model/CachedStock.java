package com.orjackson.stocktracker.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CachedStock(BigDecimal price, Instant createdAt) {
}
