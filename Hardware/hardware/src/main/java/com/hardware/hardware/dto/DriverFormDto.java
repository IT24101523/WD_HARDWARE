package com.hardware.hardware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverFormDto {

    private Long id;

    @NotBlank(message = "Driver full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+94|0)?[0-9\\s\\-]{9,12}$", message = "Please enter a valid phone number (e.g. 0771234567 or +94771234567)")
    private String phone;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String status; // 'AVAILABLE', 'ON_DELIVERY'
    private Long assignedVehicleId;
}
