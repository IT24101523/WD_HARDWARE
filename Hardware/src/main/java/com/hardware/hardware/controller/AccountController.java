package com.hardware.hardware.controller;

import com.hardware.hardware.dto.PasswordUpdateDto;
import com.hardware.hardware.dto.ProfileUpdateDto;
import com.hardware.hardware.model.Order;
import com.hardware.hardware.model.User;
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

import java.util.List;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping
    public String showAccountDashboard(@RequestParam(value = "tab", required = false, defaultValue = "dashboard") String activeTab,
                                       @RequestParam(value = "success", required = false) String success,
                                       Authentication authentication,
                                       Model model) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login?gatekeeper=true";
        }

        List<Order> userOrders = orderService.getUserOrders(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("userOrders", userOrders);
        model.addAttribute("activeTab", activeTab);

        if (!model.containsAttribute("profileUpdateDto")) {
            ProfileUpdateDto profileDto = ProfileUpdateDto.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .build();
            model.addAttribute("profileUpdateDto", profileDto);
        }

        if (!model.containsAttribute("passwordUpdateDto")) {
            model.addAttribute("passwordUpdateDto", new PasswordUpdateDto());
        }

        if ("profile".equalsIgnoreCase(success)) {
            model.addAttribute("profileSuccess", "Account profile details updated successfully!");
        } else if ("password".equalsIgnoreCase(success)) {
            model.addAttribute("passwordSuccess", "Your password has been changed successfully!");
        }

        return "account";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateDto") ProfileUpdateDto profileUpdateDto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Please fix form validation errors." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateDto", bindingResult);
            redirectAttributes.addFlashAttribute("profileUpdateDto", profileUpdateDto);
            redirectAttributes.addFlashAttribute("profileError", errorMsg);
            return "redirect:/account?tab=details";
        }

        try {
            userService.updateUserProfile(user, profileUpdateDto);
            return "redirect:/account?tab=details&success=profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
            redirectAttributes.addFlashAttribute("profileUpdateDto", profileUpdateDto);
            return "redirect:/account?tab=details";
        }
    }

    @PostMapping("/update-password")
    public String updatePassword(@Valid @ModelAttribute("passwordUpdateDto") PasswordUpdateDto passwordUpdateDto,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Invalid password input." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("passwordError", errorMsg);
            redirectAttributes.addFlashAttribute("passwordUpdateDto", passwordUpdateDto);
            return "redirect:/account?tab=details";
        }

        try {
            userService.updateUserPassword(user, passwordUpdateDto);
            return "redirect:/account?tab=details&success=password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
            redirectAttributes.addFlashAttribute("passwordUpdateDto", passwordUpdateDto);
            return "redirect:/account?tab=details";
        }
    }

    @GetMapping({"/invoice/{id}", "/account/invoice/{id}"})
    public String showInvoice(@PathVariable("id") Long id,
                              Authentication authentication,
                              Model model) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_ADMIN") || a.getAuthority().startsWith("ROLE_FLEET") || a.getAuthority().startsWith("ROLE_INVENTORY"));

        if (!isStaff && !order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access Denied: You do not have permission to view this order invoice.");
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", user);
        return "invoice";
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userService.findByUsername(authentication.getName()).orElse(null);
    }
}
