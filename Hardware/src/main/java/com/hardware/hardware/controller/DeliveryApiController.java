package com.hardware.hardware.controller;

import com.hardware.hardware.dto.DeliveryCalculationResponseDto;
import com.hardware.hardware.model.CartItem;
import com.hardware.hardware.model.User;
import com.hardware.hardware.service.CartService;
import com.hardware.hardware.service.DeliveryCalculationService;
import com.hardware.hardware.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryApiController {

    private final DeliveryCalculationService deliveryCalculationService;
    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/calculate")
    public ResponseEntity<?> calculateDeliveryFee(@RequestBody DeliveryCalculateRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please log in to calculate delivery charge."));
        }

        List<CartItem> cartItems = cartService.getCartItems(user);
        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Your cart is empty."));
        }

        DeliveryCalculationResponseDto response = deliveryCalculationService.calculateDeliveryFee(
                cartItems, request.getFulfillmentType(), request.getDistanceKm()
        );

        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        return userOpt.orElse(null);
    }

    @Data
    public static class DeliveryCalculateRequest {
        private String fulfillmentType;
        private BigDecimal distanceKm;
    }
}
