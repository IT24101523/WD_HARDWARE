package com.hardware.hardware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {

    @NotBlank(message = "Fulfillment type is required (PICKUP or DELIVERY)")
    private String fulfillmentType; // 'PICKUP' or 'DELIVERY'

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String companyName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Street address is required")
    @Size(min = 5, max = 255, message = "Street address must be between 5 and 255 characters")
    private String streetAddress;

    private String apartment;
    private String postcode;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    private String orderNotes;

    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+94|0)?[0-9\\s\\-]{9,12}$", message = "Please enter a valid phone number (e.g. 0771234567 or +94771234567)")
    private String phone;
    private BigDecimal distanceKm;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // 'COD' or 'CARD' or 'BANK'

    // Simulated card fields
    private String cardNumber;
    private String cardExpiry;
    private String cardCvc;
}
