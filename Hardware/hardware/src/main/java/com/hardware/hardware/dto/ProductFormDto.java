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
public class ProductFormDto {

    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal discountPercent;

    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Stock quantity cannot be negative")
    private BigDecimal stockQuantity;

    @NotBlank(message = "Unit of measurement is required")
    private String unit; // 'pcs', 'kg', 'kubs', 'm', 'l'

    @NotNull(message = "Unit weight in KG is required")
    @DecimalMin(value = "0.001", message = "Unit weight must be greater than 0")
    private BigDecimal unitWeightKg;

    @NotNull(message = "Unit volume in Kubs is required")
    @DecimalMin(value = "0.0001", message = "Unit volume must be greater than 0")
    private BigDecimal unitVolumeKubs;

    @NotNull(message = "Low stock safety threshold is required")
    @DecimalMin(value = "0.0", message = "Low stock threshold cannot be negative")
    private BigDecimal lowStockThreshold;

    private Long supplierId;

    private String imageUrl;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Boolean isFeatured;
}
