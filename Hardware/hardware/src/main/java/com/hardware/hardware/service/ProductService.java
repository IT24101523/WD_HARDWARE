package com.hardware.hardware.service;

import com.hardware.hardware.dto.ProductFormDto;
import com.hardware.hardware.model.Category;
import com.hardware.hardware.model.Product;
import com.hardware.hardware.model.Supplier;
import com.hardware.hardware.repository.CategoryRepository;
import com.hardware.hardware.repository.ProductRepository;
import com.hardware.hardware.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrue();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword.trim(), keyword.trim());
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    @Transactional
    public Product saveProductFromDto(ProductFormDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + dto.getCategoryId()));

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        }

        Product product;
        if (dto.getId() != null) {
            product = productRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + dto.getId()));
        } else {
            product = new Product();
        }

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPercent(dto.getDiscountPercent() != null ? dto.getDiscountPercent() : BigDecimal.ZERO);

        String unit = dto.getUnit().trim();
        BigDecimal stock = dto.getStockQuantity();
        if ("pcs".equalsIgnoreCase(unit) && stock != null) {
            stock = stock.setScale(0, RoundingMode.HALF_UP);
        }
        product.setStockQuantity(stock);
        product.setUnit(unit);

        product.setUnitWeightKg(dto.getUnitWeightKg() != null ? dto.getUnitWeightKg() : BigDecimal.ONE);
        product.setUnitVolumeKubs(dto.getUnitVolumeKubs() != null ? dto.getUnitVolumeKubs() : new BigDecimal("0.0100"));
        product.setLowStockThreshold(dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : new BigDecimal("10.00"));
        product.setSupplier(supplier);

        product.setImageUrl((dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) ? dto.getImageUrl().trim() : "https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?w=500");
        product.setCategory(category);
        product.setIsFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false);

        return productRepository.save(product);
    }

    @Transactional
    public Product restockProduct(Long id, BigDecimal addQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        if (addQuantity == null || addQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Restock quantity must be greater than zero.");
        }

        BigDecimal newStock = product.getStockQuantity().add(addQuantity);
        if ("pcs".equalsIgnoreCase(product.getUnit())) {
            newStock = newStock.setScale(0, RoundingMode.HALF_UP);
        }
        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
