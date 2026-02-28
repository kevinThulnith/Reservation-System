package com.oceanview.reservation_system.service.pricing;

import java.math.BigDecimal;

public interface RoomPricingStrategy {
    BigDecimal getDefaultNightlyRate();
}
