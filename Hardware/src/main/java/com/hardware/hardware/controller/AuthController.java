package com.hardware.hardware.controller;

import com.hardware.hardware.dto.UserRegistrationDto;
import com.hardware.hardware.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showAuthPage(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               @RequestParam(value = "registered", required = false) String registered,
                               @RequestParam(value = "tab", required = false, defaultValue = "login") String activeTab,
                               Model model) {
        if (!model.containsAttribute("registrationDto")) {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setRole("ROLE_CUSTOMER");
            model.addAttribute("registrationDto", dto);
        }
        if (error != null) {
            model.addAttribute("loginError", "Invalid username/email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("registrationSuccess", "Registration successful! You can now log in with your new credentials.");
        }
        model.addAttribute("activeTab", activeTab);
        return "login";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") UserRegistrationDto registrationDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        if (registrationDto.getRole() == null || registrationDto.getRole().isBlank()) {
            registrationDto.setRole("ROLE_CUSTOMER");
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Invalid registration input." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("registrationError", errorMsg);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registrationDto", bindingResult);
            redirectAttributes.addFlashAttribute("registrationDto", registrationDto);
            return "redirect:/login?tab=register";
        }

        try {
            userService.registerUser(registrationDto);
            return "redirect:/login?registered=true&tab=login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("registrationError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registrationDto", registrationDto);
            return "redirect:/login?tab=register";
        }
    }
}
