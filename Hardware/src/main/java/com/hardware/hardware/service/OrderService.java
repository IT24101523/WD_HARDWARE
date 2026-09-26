package com.hardware.hardware.service;

import com.hardware.hardware.dto.DeliveryCalculationResponseDto;
import com.hardware.hardware.dto.OrderRequestDto;
import com.hardware.hardware.model.*;
import com.hardware.hardware.repository.CartItemRepository;
import com.hardware.hardware.repository.CustomerRepository;
import com.hardware.hardware.repository.OrderRepository;
import com.hardware.hardware.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryCalculationService deliveryCalculationService;
    private final LogisticsService logisticsService;

    @Transactional
    public Order placeOrder(User user, OrderRequestDto orderDto) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Your cart is empty! Add products before checking out.");
        }

        // Validate Stock
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (item.getQuantity().compareTo(product.getStockQuantity()) > 0) {
                throw new IllegalStateException("Insufficient stock for product '" + product.getName() + "'. Available: " + product.getStockQuantity() + " " + product.getUnit());
            }
        }

        String fulfillmentType = orderDto.getFulfillmentType();
        if (fulfillmentType == null || fulfillmentType.isBlank()) {
            fulfillmentType = "DELIVERY";
        }
        fulfillmentType = fulfillmentType.toUpperCase().trim();

        // Perform Delivery Calculation
        DeliveryCalculationResponseDto calc = deliveryCalculationService.calculateDeliveryFee(
                cartItems, fulfillmentType, orderDto.getDistanceKm()
        );

        // Generate Order Number HW-YYYYMMDD-XXXX
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "HW-" + dateStr + "-" + randomStr;


        // Generate Unique 4-Digit Order Verification PIN
        String pickupPin = logisticsService.generate4DigitPin();

        String shippingAddr;
        if ("PICKUP".equalsIgnoreCase(fulfillmentType)) {
            shippingAddr = "COLLECT FROM SHOP (30 Galle - Colombo Rd, Balapitiya)";
        } else if (orderDto.getStreetAddress() != null && !orderDto.getStreetAddress().isBlank()) {
            StringBuilder sb = new StringBuilder();
            sb.append(orderDto.getStreetAddress().trim());
            if (orderDto.getApartment() != null && !orderDto.getApartment().isBlank()) {
                sb.append(", ").append(orderDto.getApartment().trim());
            }
            if (orderDto.getCity() != null && !orderDto.getCity().isBlank()) {
                sb.append(", ").append(orderDto.getCity().trim());
            }
            if (orderDto.getPostcode() != null && !orderDto.getPostcode().isBlank()) {
                sb.append(" ").append(orderDto.getPostcode().trim());
            }
            shippingAddr = sb.toString();
        } else if (orderDto.getShippingAddress() != null && !orderDto.getShippingAddress().isBlank()) {
            shippingAddr = orderDto.getShippingAddress().trim();
        } else {
            shippingAddr = user.getAddress();
        }

        String phone = (orderDto.getPhone() != null && !orderDto.getPhone().isBlank())
                ? orderDto.getPhone().trim()
                : user.getPhone();

        Customer customerUser = (user instanceof Customer c) ? c : customerRepository.findById(user.getId()).orElse(null);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(customerUser)
                .fulfillmentType(fulfillmentType)
                .hardwareSubtotal(calc.getHardwareSubtotal())
                .deliveryCharge(calc.getDeliveryCharge())
                .totalAmount(calc.getGrandTotal())
                .totalWeightKg(calc.getTotalWeightKg())
                .totalVolumeKubs(calc.getTotalVolumeKubs())
                .distanceKm(calc.getDistanceKm())
                .latitude(orderDto.getLatitude())
                .longitude(orderDto.getLongitude())
                .pickupPin(pickupPin)
                .shippingAddress(shippingAddr)
                .phone(phone)
                .paymentMethod(orderDto.getPaymentMethod().toUpperCase())
                .paymentStatus(orderDto.getPaymentMethod().equalsIgnoreCase("CARD") ? "PAID" : "PENDING")
                .orderStatus("PENDING")
                .orderItems(new ArrayList<>())
                .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = product.getDiscountedPrice();
            BigDecimal subtotal = unitPrice.multiply(cartItem.getQuantity());

            // Deduct stock quantity in exact ordered amount
            BigDecimal remainingStock = product.getStockQuantity().subtract(cartItem.getQuantity());
            if ("pcs".equalsIgnoreCase(product.getUnit()) && remainingStock != null) {
                remainingStock = remainingStock.setScale(0, RoundingMode.HALF_UP);
            }
            product.setStockQuantity(remainingStock);
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .unit(product.getUnit())
                    .unitWeightKg(product.getUnitWeightKg() != null ? product.getUnitWeightKg() : BigDecimal.ONE)
                    .unitVolumeKubs(product.getUnitVolumeKubs() != null ? product.getUnitVolumeKubs() : new BigDecimal("0.0100"))
                    .quantity(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Clear user cart
        cartItemRepository.deleteByUserId(user.getId());

        return savedOrder;
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        
        String oldStatus = order.getOrderStatus();
        String targetStatus = newStatus != null ? newStatus.toUpperCase().trim() : "";

        if ("CANCELLED".equalsIgnoreCase(targetStatus) && !"CANCELLED".equalsIgnoreCase(oldStatus)) {
            // Restore inventory stock for cancelled order items
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    BigDecimal restoredStock = product.getStockQuantity().add(item.getQuantity());
                    if ("pcs".equalsIgnoreCase(product.getUnit()) && restoredStock != null) {
                        restoredStock = restoredStock.setScale(0, RoundingMode.HALF_UP);
                    }
                    product.setStockQuantity(restoredStock);
                    productRepository.save(product);
                }
            }
        } else if (!"CANCELLED".equalsIgnoreCase(targetStatus) && "CANCELLED".equalsIgnoreCase(oldStatus)) {
            // Re-deduct inventory stock if order is un-cancelled
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    if (item.getQuantity().compareTo(product.getStockQuantity()) > 0) {
                        throw new IllegalStateException("Insufficient stock for product '" + product.getName() + "' to reinstate order.");
                    }
                    BigDecimal newStock = product.getStockQuantity().subtract(item.getQuantity());
                    if ("pcs".equalsIgnoreCase(product.getUnit()) && newStock != null) {
                        newStock = newStock.setScale(0, RoundingMode.HALF_UP);
                    }
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                }
            }
        }

        order.setOrderStatus(targetStatus);
        if ("DELIVERED".equalsIgnoreCase(targetStatus)) {
            order.setPaymentStatus("PAID");
        }
        return orderRepository.save(order);
    }

    // Revenue Disaggregation
    public BigDecimal getHardwareRevenue() {
        return orderRepository.calculateHardwareRevenue();
    }

    public BigDecimal getDeliveryRevenue() {
        return orderRepository.calculateDeliveryRevenue();
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.calculateTotalRevenue();
    }

    public BigDecimal getMonthlyHardwareRevenue(int year, int month) {
        return orderRepository.calculateMonthlyHardwareRevenue(year, month);
    }

    public BigDecimal getMonthlyDeliveryRevenue(int year, int month) {
        return orderRepository.calculateMonthlyDeliveryRevenue(year, month);
    }

    public BigDecimal getMonthlyTotalRevenue(int year, int month) {
        return orderRepository.calculateMonthlyTotalRevenue(year, month);
    }

    public BigDecimal getYearlyHardwareRevenue(int year) {
        return orderRepository.calculateYearlyHardwareRevenue(year);
    }

    public BigDecimal getYearlyDeliveryRevenue(int year) {
        return orderRepository.calculateYearlyDeliveryRevenue(year);
    }

    public BigDecimal getYearlyTotalRevenue(int year) {
        return orderRepository.calculateYearlyTotalRevenue(year);
    }

    public long getOrderCount() {
        return orderRepository.count();
    }
}
