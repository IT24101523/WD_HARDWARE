package com.hardware.hardware.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private Customer user;

    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private String fulfillmentType; // 'PICKUP' or 'DELIVERY'

    @Column(name = "hardware_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal hardwareSubtotal;

    @Column(name = "delivery_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(name = "total_volume_kubs", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalVolumeKubs;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_vehicle_id")
    private FleetVehicle assignedVehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @Column(name = "assigned_driver_name", length = 100)
    private String assignedDriverName;

    @Column(name = "pickup_pin", length = 10)
    private String pickupPin;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // 'COD' or 'CARD'

    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus; // 'PENDING', 'PAID'

    @Column(name = "order_status", nullable = false, length = 50)
    private String orderStatus; // 'PENDING', 'DELIVERING', 'DELIVERED', 'CANCELLED'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "PENDING";
        }
        if (this.orderStatus == null) {
            this.orderStatus = "PENDING";
        }
        if (this.fulfillmentType == null) {
            this.fulfillmentType = "DELIVERY";
        }
    }
}
