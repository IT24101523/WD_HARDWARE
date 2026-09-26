package com.hardware.hardware.service;

import com.hardware.hardware.dto.DeliveryCalculationResponseDto;
import com.hardware.hardware.model.CartItem;
import com.hardware.hardware.model.FleetVehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeliveryCalculationService {

    /**
     * Vehicle Capability & Rate Matrix Standards
     * 1. BIKE:         Max 15.00 kg   | Max 0.05 kubs | Base LKR 250.00  | Rate LKR 50.00/km
     * 2. TUKTUK:       Max 150.00 kg  | Max 0.20 kubs | Base LKR 500.00  | Rate LKR 80.00/km
     * 3. LIGHT_TRUCK:  Max 1200.00 kg | Max 1.50 kubs | Base LKR 1500.00 | Rate LKR 150.00/km
     * 4. TIPPER:       Max 8000.00 kg | Max 5.00 kubs | Base LKR 4000.00 | Rate LKR 300.00/km
     */
    public DeliveryCalculationResponseDto calculateDeliveryFee(List<CartItem> cartItems, String fulfillmentType, BigDecimal distanceKm) {
        BigDecimal hardwareSubtotal = BigDecimal.ZERO;
        BigDecimal totalWeightKg = BigDecimal.ZERO;
        BigDecimal totalVolumeKubs = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            BigDecimal qty = item.getQuantity();
            BigDecimal price = item.getProduct().getDiscountedPrice();
            BigDecimal weight = (item.getProduct().getUnitWeightKg() != null) ? item.getProduct().getUnitWeightKg() : BigDecimal.ONE;
            BigDecimal volume = (item.getProduct().getUnitVolumeKubs() != null) ? item.getProduct().getUnitVolumeKubs() : new BigDecimal("0.0100");

            hardwareSubtotal = hardwareSubtotal.add(price.multiply(qty));
            totalWeightKg = totalWeightKg.add(weight.multiply(qty));
            totalVolumeKubs = totalVolumeKubs.add(volume.multiply(qty));
        }

        hardwareSubtotal = hardwareSubtotal.setScale(2, RoundingMode.HALF_UP);
        totalWeightKg = totalWeightKg.setScale(2, RoundingMode.HALF_UP);
        totalVolumeKubs = totalVolumeKubs.setScale(4, RoundingMode.HALF_UP);

        if ("PICKUP".equalsIgnoreCase(fulfillmentType)) {
            return DeliveryCalculationResponseDto.builder()
                    .fulfillmentType("PICKUP")
                    .hardwareSubtotal(hardwareSubtotal)
                    .totalWeightKg(totalWeightKg)
                    .totalVolumeKubs(totalVolumeKubs)
                    .distanceKm(BigDecimal.ZERO)
                    .matchedVehicleType("STORE_PICKUP")
                    .matchedVehicleModel("Customer Self Pickup")
                    .vehicleMaxWeightKg(BigDecimal.ZERO)
                    .vehicleMaxVolumeKubs(BigDecimal.ZERO)
                    .baseFee(BigDecimal.ZERO)
                    .ratePerKm(BigDecimal.ZERO)
                    .deliveryCharge(BigDecimal.ZERO)
                    .grandTotal(hardwareSubtotal)
                    .availableVehicleFound(true)
                    .warningMessage(null)
                    .build();
        }

        // Distance validation
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            distanceKm = BigDecimal.ZERO;
        }
        distanceKm = distanceKm.setScale(2, RoundingMode.HALF_UP);

        // Vehicle Capability Matching Logic
        String matchedType;
        String matchedModel;
        BigDecimal maxW;
        BigDecimal maxV;
        BigDecimal baseFee;
        BigDecimal ratePerKm;

        if (totalWeightKg.compareTo(new BigDecimal("15.00")) <= 0 && totalVolumeKubs.compareTo(new BigDecimal("0.0500")) <= 0) {
            matchedType = "BIKE";
            matchedModel = "Motorcycle (Bike Express)";
            maxW = new BigDecimal("15.00");
            maxV = new BigDecimal("0.05");
            baseFee = new BigDecimal("250.00");
            ratePerKm = new BigDecimal("50.00");
        } else if (totalWeightKg.compareTo(new BigDecimal("150.00")) <= 0 && totalVolumeKubs.compareTo(new BigDecimal("0.2000")) <= 0) {
            matchedType = "TUKTUK";
            matchedModel = "Diesel TukTuk Cargo";
            maxW = new BigDecimal("150.00");
            maxV = new BigDecimal("0.20");
            baseFee = new BigDecimal("500.00");
            ratePerKm = new BigDecimal("80.00");
        } else if (totalWeightKg.compareTo(new BigDecimal("1200.00")) <= 0 && totalVolumeKubs.compareTo(new BigDecimal("1.5000")) <= 0) {
            matchedType = "LIGHT_TRUCK";
            matchedModel = "Light Truck (Dimo Batta)";
            maxW = new BigDecimal("1200.00");
            maxV = new BigDecimal("1.50");
            baseFee = new BigDecimal("1500.00");
            ratePerKm = new BigDecimal("150.00");
        } else if (totalWeightKg.compareTo(new BigDecimal("8000.00")) <= 0 && totalVolumeKubs.compareTo(new BigDecimal("5.0000")) <= 0) {
            matchedType = "TIPPER";
            matchedModel = "Tipper Truck (Heavy Dump)";
            maxW = new BigDecimal("8000.00");
            maxV = new BigDecimal("5.00");
            baseFee = new BigDecimal("4000.00");
            ratePerKm = new BigDecimal("300.00");
        } else {
            // Overweight/Overvolume warning fallback
            matchedType = "MULTI_TRUCK_REQUIRED";
            matchedModel = "Heavy Special Dispatch (Exceeds Single Tipper)";
            maxW = new BigDecimal("8000.00");
            maxV = new BigDecimal("5.00");
            baseFee = new BigDecimal("4000.00");
            ratePerKm = new BigDecimal("300.00");
        }

        // Delivery Charge = Base Fee + (Distance * Rate per KM)
        BigDecimal distanceCost = distanceKm.multiply(ratePerKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryCharge = baseFee.add(distanceCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = hardwareSubtotal.add(deliveryCharge).setScale(2, RoundingMode.HALF_UP);

        return DeliveryCalculationResponseDto.builder()
                .fulfillmentType("DELIVERY")
                .hardwareSubtotal(hardwareSubtotal)
                .totalWeightKg(totalWeightKg)
                .totalVolumeKubs(totalVolumeKubs)
                .distanceKm(distanceKm)
                .matchedVehicleType(matchedType)
                .matchedVehicleModel(matchedModel)
                .vehicleMaxWeightKg(maxW)
                .vehicleMaxVolumeKubs(maxV)
                .baseFee(baseFee)
                .ratePerKm(ratePerKm)
                .deliveryCharge(deliveryCharge)
                .grandTotal(grandTotal)
                .availableVehicleFound(true)
                .build();
    }
}
