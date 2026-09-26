package com.hardware.hardware.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCalculationResponseDto {
    private String fulfillmentType; // 'PICKUP' or 'DELIVERY'
    private BigDecimal hardwareSubtotal;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeKubs;
    private BigDecimal distanceKm;
    private String matchedVehicleType; // 'BIKE', 'TUKTUK', 'LIGHT_TRUCK', 'TIPPER'
    private String matchedVehicleModel;
    private BigDecimal vehicleMaxWeightKg;
    private BigDecimal vehicleMaxVolumeKubs;
    private BigDecimal baseFee;
    private BigDecimal ratePerKm;
    private BigDecimal deliveryCharge;
    private BigDecimal grandTotal;
    private boolean availableVehicleFound;
    private String warningMessage;
}
