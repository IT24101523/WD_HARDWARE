package com.hardware.hardware.controller;

import com.hardware.hardware.model.CartItem;
import com.hardware.hardware.model.Product;
import com.hardware.hardware.model.User;
import com.hardware.hardware.service.CartService;
import com.hardware.hardware.service.ProductService;
import com.hardware.hardware.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProductDetailController {

    private final ProductService productService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping("/product/{id}")
    public String showProductDetails(@PathVariable("id") Long id,
                                     Authentication authentication,
                                     Model model) {

        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        model.addAttribute("product", product);

        User user = getAuthenticatedUser(authentication);
        if (user != null) {
            List<CartItem> cartItems = cartService.getCartItems(user);
            BigDecimal cartTotal = cartService.getCartTotal(user);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartTotal", cartTotal);
            model.addAttribute("cartCount", cartItems.size());
            model.addAttribute("currentUser", user);
        } else {
            model.addAttribute("cartItems", List.of());
            model.addAttribute("cartTotal", BigDecimal.ZERO);
            model.addAttribute("cartCount", 0);
        }

        // Related products in same category
        List<Product> relatedProducts = productService.getProductsByCategory(product.getCategory().getId());
        model.addAttribute("relatedProducts", relatedProducts);

        return "product-details";
    }

    @org.springframework.web.bind.annotation.PostMapping("/cart/add")
    public String addToCartForm(@org.springframework.web.bind.annotation.RequestParam("productId") Long productId,
                                @org.springframework.web.bind.annotation.RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                Authentication authentication,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("cartError", "Please log in to add items to your cart.");
            return "redirect:/login";
        }
        try {
            cartService.addToCart(user, productId, java.math.BigDecimal.valueOf(quantity != null ? quantity : 1));
            redirectAttributes.addFlashAttribute("cartSuccess", "Added " + quantity + " item(s) to your shopping cart!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }
        return "redirect:/product/" + productId;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        return userOpt.orElse(null);
    }
}
