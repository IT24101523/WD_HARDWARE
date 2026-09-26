package com.hardware.hardware.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType; // 'BIKE', 'TUKTUK', 'LIGHT_TRUCK', 'TIPPER'

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "license_plate", nullable = false, unique = true, length = 30)
    private String licensePlate;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "max_volume_kubs", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxVolumeKubs;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "per_km_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal perKmRate;

    @Column(nullable = false, length = 30)
    private String status; // 'AVAILABLE', 'DELIVERING'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
