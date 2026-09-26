package com.hardware.hardware.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetVehicleFormDto {

    private Long id;

    private String vehicleType; // Optional, derived from modelName

    @NotBlank(message = "Vehicle type / Model is required")
    private String modelName;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotNull(message = "Max weight capacity is required")
    @DecimalMin(value = "1.0", message = "Max weight must be greater than 0")
    private BigDecimal maxWeightKg;

    @NotNull(message = "Max volume capacity is required")
    @DecimalMin(value = "0.01", message = "Max volume must be greater than 0")
    private BigDecimal maxVolumeKubs;

    @NotNull(message = "Fixed base fee is required")
    @DecimalMin(value = "0.0", message = "Base fee cannot be negative")
    private BigDecimal baseFee;

    @NotNull(message = "Distance rate per KM is required")
    @DecimalMin(value = "0.0", message = "Rate per KM cannot be negative")
    private BigDecimal perKmRate;

    private String status; // 'AVAILABLE', 'DELIVERING'
}
