package com.hardware.hardware.repository;

import com.hardware.hardware.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByPickupPin(String pickupPin);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAllByOrderByCreatedAtDesc();

    // All-time revenue queries
    @Query("SELECT COALESCE(SUM(o.hardwareSubtotal), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED'")
    BigDecimal calculateHardwareRevenue();

    @Query("SELECT COALESCE(SUM(o.deliveryCharge), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED'")
    BigDecimal calculateDeliveryRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED'")
    BigDecimal calculateTotalRevenue();

    // Monthly revenue queries
    @Query("SELECT COALESCE(SUM(o.hardwareSubtotal), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    BigDecimal calculateMonthlyHardwareRevenue(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.deliveryCharge), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    BigDecimal calculateMonthlyDeliveryRevenue(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    BigDecimal calculateMonthlyTotalRevenue(@Param("year") int year, @Param("month") int month);

    // Yearly revenue queries
    @Query("SELECT COALESCE(SUM(o.hardwareSubtotal), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year")
    BigDecimal calculateYearlyHardwareRevenue(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(o.deliveryCharge), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year")
    BigDecimal calculateYearlyDeliveryRevenue(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus != 'CANCELLED' AND YEAR(o.createdAt) = :year")
    BigDecimal calculateYearlyTotalRevenue(@Param("year") int year);

    long countByOrderStatus(String orderStatus);
    long countByUserId(Long userId);
}
