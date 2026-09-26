package com.hardware.hardware.controller;

import com.hardware.hardware.dto.DeliveryCalculationResponseDto;
import com.hardware.hardware.dto.OrderRequestDto;
import com.hardware.hardware.model.CartItem;
import com.hardware.hardware.model.Order;
import com.hardware.hardware.model.User;
import com.hardware.hardware.service.CartService;
import com.hardware.hardware.service.DeliveryCalculationService;
import com.hardware.hardware.service.OrderService;
import com.hardware.hardware.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;
    private final DeliveryCalculationService deliveryCalculationService;

    @GetMapping
    public String showCheckoutPage(@RequestParam(value = "orderSuccess", required = false) String orderNumber,
                                   Authentication authentication,
                                   Model model) {
        User user = getAuthenticatedUser(authentication);
        // Mandatory Authentication Gatekeeper
        if (user == null) {
            return "redirect:/login?gatekeeper=true";
        }

        List<CartItem> cartItems = cartService.getCartItems(user);
        BigDecimal hardwareSubtotal = cartService.getCartTotal(user);

        if (cartItems.isEmpty() && orderNumber == null) {
            return "redirect:/index";
        }

        if (!model.containsAttribute("orderRequestDto")) {
            String[] nameParts = user.getFullName() != null ? user.getFullName().split(" ", 2) : new String[]{"", ""};
            String first = nameParts.length > 0 ? nameParts[0] : "";
            String last = nameParts.length > 1 ? nameParts[1] : "";

            OrderRequestDto dto = OrderRequestDto.builder()
                    .fulfillmentType("DELIVERY")
                    .firstName(first)
                    .lastName(last)
                    .email(user.getEmail())
                    .city("Balapitiya")
                    .streetAddress(user.getAddress())
                    .phone(user.getPhone())
                    .distanceKm(new BigDecimal("1.00")) // Balapitiya local distance 1km
                    .paymentMethod("BANK")
                    .build();
            model.addAttribute("orderRequestDto", dto);
        }

        // Calculate initial delivery estimate based on 1.00km for Balapitiya
        DeliveryCalculationResponseDto initialCalc = deliveryCalculationService.calculateDeliveryFee(
                cartItems, "DELIVERY", new BigDecimal("1.00")
        );

        model.addAttribute("currentUser", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("hardwareSubtotal", hardwareSubtotal);
        model.addAttribute("initialCalc", initialCalc);

        if (orderNumber != null) {
            Optional<Order> placedOrderOpt = orderService.getOrderByNumber(orderNumber);
            placedOrderOpt.ifPresent(order -> model.addAttribute("placedOrder", order));
        }

        return "checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(@Valid @ModelAttribute("orderRequestDto") OrderRequestDto orderRequestDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?gatekeeper=true";
        }

        List<CartItem> cartItems = cartService.getCartItems(user);
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("orderError", "Your cart is empty or this order has already been completed.");
            return "redirect:/account?tab=orders";
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Please check all required billing fields." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("orderError", errorMsg);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.orderRequestDto", bindingResult);
            redirectAttributes.addFlashAttribute("orderRequestDto", orderRequestDto);
            return "redirect:/checkout";
        }

        try {
            Order order = orderService.placeOrder(user, orderRequestDto);
            redirectAttributes.addFlashAttribute("orderPlacedSuccess", "Order #" + order.getOrderNumber() + " placed successfully!");
            return "redirect:/checkout?orderSuccess=" + order.getOrderNumber();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderError", e.getMessage());
            redirectAttributes.addFlashAttribute("orderRequestDto", orderRequestDto);
            return "redirect:/checkout";
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userService.findByUsername(authentication.getName()).orElse(null);
    }
}
