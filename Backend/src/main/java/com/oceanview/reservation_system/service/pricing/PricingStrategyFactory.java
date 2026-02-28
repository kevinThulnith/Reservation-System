package com.oceanview.reservation_system.service.pricing;

import com.oceanview.reservation_system.model.Room.RoomType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PricingStrategyFactory {

    private final Map<RoomType, RoomPricingStrategy> strategies;

    public PricingStrategyFactory(
            SingleRoomPricing single,
            DoubleRoomPricing doublePricing,
            SuiteRoomPricing suite,
            DeluxeRoomPricing deluxe,
            PenthouseRoomPricing penthouse) {

        strategies = Map.of(
                RoomType.SINGLE,     single,
                RoomType.DOUBLE,     doublePricing,
                RoomType.SUITE,      suite,
                RoomType.DELUXE,     deluxe,
                RoomType.PENTHOUSE,  penthouse
        );
    }

    public RoomPricingStrategy getStrategy(RoomType roomType) {
        RoomPricingStrategy strategy = strategies.get(roomType);
        if (strategy == null) {
            throw new IllegalArgumentException("No pricing strategy found for room type: " + roomType);
        }
        return strategy;
    }
}
