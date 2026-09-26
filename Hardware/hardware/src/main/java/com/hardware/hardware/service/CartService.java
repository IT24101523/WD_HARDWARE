package com.hardware.hardware.service;

import com.hardware.hardware.model.CartItem;
import com.hardware.hardware.model.Customer;
import com.hardware.hardware.model.Product;
import com.hardware.hardware.model.User;
import com.hardware.hardware.repository.CartItemRepository;
import com.hardware.hardware.repository.CustomerRepository;
import com.hardware.hardware.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUserId(user.getId());
    }

    @Transactional
    public CartItem addToCart(User user, Long productId, BigDecimal quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (quantity == null || quantity.compareTo(BigDecimal.ONE) < 0) {
            quantity = BigDecimal.ONE;
        }

        if ("pcs".equalsIgnoreCase(product.getUnit())) {
            quantity = quantity.setScale(0, RoundingMode.HALF_UP);
        }

        Optional<CartItem> existingOpt = cartItemRepository.findByUserIdAndProductId(user.getId(), productId);

        if (existingOpt.isPresent()) {
            CartItem existing = existingOpt.get();
            BigDecimal newQty = existing.getQuantity().add(quantity);
            if (newQty.compareTo(product.getStockQuantity()) > 0) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock (" + product.getStockQuantity() + " " + product.getUnit() + ")");
            }
            existing.setQuantity(newQty);
            return cartItemRepository.save(existing);
        } else {
            if (quantity.compareTo(product.getStockQuantity()) > 0) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock (" + product.getStockQuantity() + " " + product.getUnit() + ")");
            }
            Customer customerUser = (user instanceof Customer c) ? c : customerRepository.findById(user.getId()).orElse(null);
            CartItem cartItem = CartItem.builder()
                    .user(customerUser)
                    .product(product)
                    .quantity(quantity)
                    .build();
            return cartItemRepository.save(cartItem);
        }
    }

    @Transactional
    public CartItem updateQuantity(User user, Long cartItemId, BigDecimal newQuantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized cart modification");
        }

        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            cartItemRepository.delete(cartItem);
            return null;
        }

        Product product = cartItem.getProduct();
        if (newQuantity.compareTo(product.getStockQuantity()) > 0) {
            throw new IllegalArgumentException("Quantity exceeds available stock (" + product.getStockQuantity() + " " + product.getUnit() + ")");
        }

        cartItem.setQuantity(newQuantity);
        return cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeFromCart(User user, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized cart modification");
        }

        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUserId(user.getId());
    }

    public BigDecimal getCartTotal(User user) {
        List<CartItem> items = getCartItems(user);
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
