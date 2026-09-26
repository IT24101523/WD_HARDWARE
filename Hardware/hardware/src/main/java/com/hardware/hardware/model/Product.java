package com.hardware.hardware.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price; // Price in LKR

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "stock_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockQuantity;

    @Column(nullable = false, length = 20)
    private String unit; // 'pcs', 'kg', 'kubs', 'm', 'l'

    @Column(name = "unit_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitWeightKg; // Weight per unit in kg

    @Column(name = "unit_volume_kubs", nullable = false, precision = 10, scale = 4)
    private BigDecimal unitVolumeKubs; // Volume per unit in kubs/m3

    @Column(name = "low_stock_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal lowStockThreshold;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public BigDecimal getDiscountedPrice() {
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountFactor = BigDecimal.valueOf(100).subtract(discountPercent)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return price.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }
}
