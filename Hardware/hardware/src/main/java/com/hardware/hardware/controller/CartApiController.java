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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;
    private final UserService userService;
    private final DeliveryCalculationService deliveryCalculationService;

    @GetMapping("/details")
    public ResponseEntity<?> getCartDetails(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        List<CartItem> items = cartService.getCartItems(user);
        BigDecimal total = cartService.getCartTotal(user);

        DeliveryCalculationResponseDto deliveryCalc = deliveryCalculationService.calculateDeliveryFee(
                items, "DELIVERY", new BigDecimal("1.00") // Local Galle District baseline 1km
        );

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", total);
        response.put("count", items.size());
        response.put("totalWeightKg", deliveryCalc.getTotalWeightKg());
        response.put("totalVolumeKubs", deliveryCalc.getTotalVolumeKubs());
        response.put("recommendedVehicle", deliveryCalc.getMatchedVehicleModel());
        response.put("recommendedVehicleType", deliveryCalc.getMatchedVehicleType());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddCartRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please log in to add items to your cart."));
        }

        try {
            cartService.addToCart(user, request.getProductId(), request.getQuantity());
            return getCartDetails(authentication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody UpdateCartRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            cartService.updateQuantity(user, request.getCartItemId(), request.getQuantity());
            return getCartDetails(authentication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeItem(@PathVariable("id") Long cartItemId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        try {
            cartService.removeFromCart(user, cartItemId);
            return getCartDetails(authentication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        return userOpt.orElse(null);
    }

    @Data
    public static class AddCartRequest {
        private Long productId;
        private BigDecimal quantity;
    }

    @Data
    public static class UpdateCartRequest {
        private Long cartItemId;
        private BigDecimal quantity;
    }
}
