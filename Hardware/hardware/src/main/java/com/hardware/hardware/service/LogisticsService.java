package com.hardware.hardware.service;

import com.hardware.hardware.model.Driver;
import com.hardware.hardware.model.FleetVehicle;
import com.hardware.hardware.model.Order;
import com.hardware.hardware.repository.DriverRepository;
import com.hardware.hardware.repository.FleetVehicleRepository;
import com.hardware.hardware.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final OrderRepository orderRepository;
    private final FleetVehicleRepository fleetVehicleRepository;
    private final DriverRepository driverRepository;
    private static final SecureRandom random = new SecureRandom();

    public String generate4DigitPin() {
        int pin = 1000 + random.nextInt(9000);
        return String.valueOf(pin);
    }

    @Transactional
    public Order assignVehicleAndDriverToOrder(Long orderId, Long vehicleId, Long driverId, String driverNameFallback) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        FleetVehicle vehicle = fleetVehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        if (!"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Vehicle " + vehicle.getLicensePlate() + " is currently " + vehicle.getStatus() + " and unavailable for dispatch!");
        }

        // Validate vehicle load capacity (Max Weight & Max Volume)
        if (vehicle.getMaxWeightKg().compareTo(order.getTotalWeightKg()) < 0 || vehicle.getMaxVolumeKubs().compareTo(order.getTotalVolumeKubs()) < 0) {
            throw new IllegalArgumentException("Cannot dispatch vehicle " + vehicle.getModelName() + " (" + vehicle.getLicensePlate() + ")! Payload capacity (Max " + vehicle.getMaxWeightKg() + " kg / " + vehicle.getMaxVolumeKubs() + " kubs) is insufficient for Order #" + order.getOrderNumber() + " (Total Load: " + order.getTotalWeightKg() + " kg / " + order.getTotalVolumeKubs() + " kubs). Please select a higher capacity vehicle.");
        }

        Driver driver = null;
        if (driverId != null) {
            driver = driverRepository.findById(driverId).orElse(null);
            if (driver != null && !"AVAILABLE".equalsIgnoreCase(driver.getStatus())) {
                throw new IllegalStateException("Driver " + driver.getFullName() + " is currently " + driver.getStatus() + "!");
            }
        }

        vehicle.setStatus("DELIVERING");
        fleetVehicleRepository.save(vehicle);

        if (driver != null) {
            driver.setStatus("ON_DELIVERY");
            driver.setAssignedVehicle(vehicle);
            driverRepository.save(driver);
            order.setAssignedDriver(driver);
            order.setAssignedDriverName(driver.getFullName());
        } else {
            String driverName = (driverNameFallback != null && !driverNameFallback.isBlank()) ? driverNameFallback.trim() : "Store Fleet Driver";
            order.setAssignedDriverName(driverName);
        }

        order.setAssignedVehicle(vehicle);
        order.setOrderStatus("DELIVERING");

        return orderRepository.save(order);
    }

    @Transactional
    public Order completeOrderDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Order #" + order.getOrderNumber() + " has already been delivered and completed!");
        }

        // Mark Order as DELIVERED & PAID
        order.setOrderStatus("DELIVERED");
        order.setPaymentStatus("PAID");

        // Release Assigned Vehicle back to AVAILABLE
        if (order.getAssignedVehicle() != null) {
            FleetVehicle vehicle = order.getAssignedVehicle();
            vehicle.setStatus("AVAILABLE");
            fleetVehicleRepository.save(vehicle);
        }

        // Release Assigned Driver back to AVAILABLE
        if (order.getAssignedDriver() != null) {
            Driver driver = order.getAssignedDriver();
            driver.setStatus("AVAILABLE");
            driver.setAssignedVehicle(null);
            driverRepository.save(driver);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order verifyPinAndCompleteDelivery(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new IllegalArgumentException("Please enter a valid 4-Digit Order Verification PIN!");
        }

        Order order = orderRepository.findByPickupPin(pin.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Order PIN '" + pin.trim() + "'! No matching order found."));

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Order #" + order.getOrderNumber() + " has already been delivered and completed!");
        }

        // Mark Order as DELIVERED & PAID
        order.setOrderStatus("DELIVERED");
        order.setPaymentStatus("PAID");

        // Release Assigned Vehicle back to AVAILABLE
        if (order.getAssignedVehicle() != null) {
            FleetVehicle vehicle = order.getAssignedVehicle();
            vehicle.setStatus("AVAILABLE");
            fleetVehicleRepository.save(vehicle);
        }

        // Release Assigned Driver back to AVAILABLE
        if (order.getAssignedDriver() != null) {
            Driver driver = order.getAssignedDriver();
            driver.setStatus("AVAILABLE");
            driver.setAssignedVehicle(null);
            driverRepository.save(driver);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order verifyOrderPinAndCompleteDelivery(Long orderId, String pin) {
        if (pin == null || pin.isBlank()) {
            throw new IllegalArgumentException("Please enter the 4-Digit Order Verification PIN!");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Order #" + order.getOrderNumber() + " has already been delivered and completed!");
        }

        if (order.getPickupPin() == null || !order.getPickupPin().trim().equals(pin.trim())) {
            throw new IllegalArgumentException("Incorrect PIN entered for Order #" + order.getOrderNumber() + "! Customer's verification PIN does not match.");
        }

        // Mark Order as DELIVERED & PAID
        order.setOrderStatus("DELIVERED");
        order.setPaymentStatus("PAID");

        // Release Assigned Vehicle back to AVAILABLE
        if (order.getAssignedVehicle() != null) {
            FleetVehicle vehicle = order.getAssignedVehicle();
            vehicle.setStatus("AVAILABLE");
            fleetVehicleRepository.save(vehicle);
        }

        // Release Assigned Driver back to AVAILABLE
        if (order.getAssignedDriver() != null) {
            Driver driver = order.getAssignedDriver();
            driver.setStatus("AVAILABLE");
            driver.setAssignedVehicle(null);
            driverRepository.save(driver);
        }

        return orderRepository.save(order);
    }

    public List<FleetVehicle> getAvailableVehicles() {
        return fleetVehicleRepository.findByStatus("AVAILABLE");
    }
}
