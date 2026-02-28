package com.oceanview.reservation_system.service.pricing;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PenthouseRoomPricing implements RoomPricingStrategy {
    @Override
    public BigDecimal getDefaultNightlyRate() {
        return new BigDecimal("500.00");
    }
}
