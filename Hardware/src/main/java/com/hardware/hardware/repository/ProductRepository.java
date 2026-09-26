package com.hardware.hardware.repository;

import com.hardware.hardware.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.Optional<Product> findByName(String name);
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByIsFeaturedTrue();
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameKeyword, String descKeyword);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    List<Product> findBySupplierId(Long supplierId);
}
