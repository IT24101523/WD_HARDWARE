package com.hardware.hardware.controller;

import com.hardware.hardware.model.Category;
import com.hardware.hardware.model.Product;
import com.hardware.hardware.model.User;
import com.hardware.hardware.service.CartService;
import com.hardware.hardware.service.CategoryService;
import com.hardware.hardware.service.ProductService;
import com.hardware.hardware.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping({"/", "/index"})
    public String homePage(@RequestParam(value = "categoryId", required = false) Long categoryId,
                           @RequestParam(value = "search", required = false) String search,
                           Authentication authentication,
                           Model model) {

        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        List<Product> featuredProducts = productService.getFeaturedProducts();
        model.addAttribute("featuredProducts", featuredProducts);

        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productService.searchProducts(search);
            model.addAttribute("currentSearch", search);
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            products = productService.getAllProducts();
        }
        model.addAttribute("products", products);

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Optional<User> currentUserOpt = userService.findByUsername(authentication.getName());
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("cartItems", cartService.getCartItems(currentUser));
                model.addAttribute("cartTotal", cartService.getCartTotal(currentUser));
            }
        } else {
            model.addAttribute("cartItems", Collections.emptyList());
            model.addAttribute("cartTotal", 0);
        }

        return "index";
    }

    @GetMapping("/api/products/filter")
    @ResponseBody
    public List<Product> filterProductsApi(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                           @RequestParam(value = "search", required = false) String search) {
        if (search != null && !search.isBlank()) {
            return productService.searchProducts(search);
        } else if (categoryId != null && categoryId > 0) {
            return productService.getProductsByCategory(categoryId);
        }
        return productService.getAllProducts();
    }
}
