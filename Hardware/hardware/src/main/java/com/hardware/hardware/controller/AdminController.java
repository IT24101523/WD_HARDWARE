package com.hardware.hardware.controller;

import com.hardware.hardware.dto.*;
import com.hardware.hardware.model.*;
import com.hardware.hardware.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final SupplierService supplierService;
    private final FleetService fleetService;
    private final LogisticsService logisticsService;
    private final ProcurementEmailService procurementEmailService;
    private final UserService userService;
    private final DriverService driverService;

    @GetMapping
    public String adminDashboard(@RequestParam(value = "tab", required = false) String activeTab,
                                 @RequestParam(value = "year", required = false) Integer yearParam,
                                 @RequestParam(value = "month", required = false) Integer monthParam,
                                 Authentication authentication,
                                 Model model) {

        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isFleetOnly = !isAdmin && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_FLEET_OFFICER"));
            boolean isInventoryOnly = !isAdmin && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_INVENTORY_OFFICER"));

            if (isFleetOnly) {
                if (activeTab == null || !activeTab.equals("fleet")) {
                    activeTab = "fleet";
                }
            } else if (isInventoryOnly) {
                if (activeTab == null || (!activeTab.equals("analytics") && !activeTab.equals("products"))) {
                    activeTab = "analytics";
                }
            } else {
                if (activeTab == null || activeTab.isBlank()) {
                    activeTab = "analytics";
                }
            }
        } else {
            if (activeTab == null || activeTab.isBlank()) {
                activeTab = "analytics";
            }
        }

        // TAB A: Analytics & Disaggregated Revenue Data
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int selectedYear = (yearParam != null && yearParam >= 2000 && yearParam <= 2100) ? yearParam : currentYear;
        int selectedMonth = (monthParam != null && monthParam >= 1 && monthParam <= 12) ? monthParam : now.getMonthValue();
        String selectedMonthName = java.time.Month.of(selectedMonth).name();

        BigDecimal hardwareRevenue = orderService.getHardwareRevenue();
        BigDecimal deliveryRevenue = orderService.getDeliveryRevenue();
        BigDecimal totalRevenue = orderService.getTotalRevenue();

        BigDecimal monthlyHardwareRevenue = orderService.getMonthlyHardwareRevenue(selectedYear, selectedMonth);
        BigDecimal monthlyDeliveryRevenue = orderService.getMonthlyDeliveryRevenue(selectedYear, selectedMonth);
        BigDecimal monthlyTotalRevenue = orderService.getMonthlyTotalRevenue(selectedYear, selectedMonth);

        BigDecimal yearlyHardwareRevenue = orderService.getYearlyHardwareRevenue(selectedYear);
        BigDecimal yearlyDeliveryRevenue = orderService.getYearlyDeliveryRevenue(selectedYear);
        BigDecimal yearlyTotalRevenue = orderService.getYearlyTotalRevenue(selectedYear);

        long totalOrders = orderService.getOrderCount();
        List<Product> lowStockProducts = productService.getLowStockProducts();

        // Available years list (e.g. current year down to 2020)
        List<Integer> availableYears = java.util.stream.IntStream.rangeClosed(2020, currentYear + 1)
                .boxed()
                .sorted(java.util.Comparator.reverseOrder())
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("hardwareRevenue", hardwareRevenue);
        model.addAttribute("deliveryRevenue", deliveryRevenue);
        model.addAttribute("totalRevenue", totalRevenue);

        model.addAttribute("monthlyHardwareRevenue", monthlyHardwareRevenue);
        model.addAttribute("monthlyDeliveryRevenue", monthlyDeliveryRevenue);
        model.addAttribute("monthlyTotalRevenue", monthlyTotalRevenue);

        model.addAttribute("yearlyHardwareRevenue", yearlyHardwareRevenue);
        model.addAttribute("yearlyDeliveryRevenue", yearlyDeliveryRevenue);
        model.addAttribute("yearlyTotalRevenue", yearlyTotalRevenue);

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthName", selectedMonthName);
        model.addAttribute("availableYears", availableYears);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("lowStockProducts", lowStockProducts);

        // TAB B: Product Management Data
        List<Product> allProducts = productService.getAllProducts();
        List<Category> allCategories = categoryService.getAllCategories();
        List<Supplier> allSuppliers = supplierService.getAllSuppliers();

        model.addAttribute("allProducts", allProducts);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("allSuppliers", allSuppliers);

        if (!model.containsAttribute("productFormDto")) {
            model.addAttribute("productFormDto", new ProductFormDto());
        }

        // TAB C: Fleet Roster, Drivers & Dispatch Logistics Data
        List<FleetVehicle> allVehicles = fleetService.getAllVehicles();
        List<FleetVehicle> availableVehicles = logisticsService.getAvailableVehicles();
        List<Driver> allDrivers = driverService.getAllDrivers();
        List<Driver> availableDrivers = driverService.getAvailableDrivers();
        List<Order> allOrders = orderService.getAllOrders();

        model.addAttribute("allVehicles", allVehicles);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("allDrivers", allDrivers);
        model.addAttribute("availableDrivers", availableDrivers);
        model.addAttribute("allOrders", allOrders);

        if (!model.containsAttribute("fleetVehicleFormDto")) {
            model.addAttribute("fleetVehicleFormDto", new FleetVehicleFormDto());
        }
        if (!model.containsAttribute("driverFormDto")) {
            model.addAttribute("driverFormDto", new DriverFormDto());
        }

        // TAB D: Supplier Directory Data
        if (!model.containsAttribute("supplierFormDto")) {
            model.addAttribute("supplierFormDto", new SupplierFormDto());
        }

        // TAB E: Staff and Customer Accounts Data
        List<User> staffUsers = userService.getStaffUsers();
        List<User> customerUsers = userService.getCustomerUsers();
        List<User> allUsers = userService.getAllUsers();
        model.addAttribute("staffUsers", staffUsers);
        model.addAttribute("customerUsers", customerUsers);
        model.addAttribute("allUsers", allUsers);

        if (!model.containsAttribute("userAdminDto")) {
            model.addAttribute("userAdminDto", new UserAdminDto());
        }
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new UserRegistrationDto());
        }

        model.addAttribute("activeTab", activeTab);

        return "admin";
    }

    // ---------------------------------------------------------
    // TAB B: PRODUCT CRUD & 1-CLICK EMAIL REQUISITION
    // ---------------------------------------------------------
    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute("productFormDto") ProductFormDto productFormDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productFormDto", bindingResult);
            redirectAttributes.addFlashAttribute("productFormDto", productFormDto);
            redirectAttributes.addFlashAttribute("productFormError", "Please fix form validation errors.");
            return "redirect:/admin?tab=products";
        }

        try {
            productService.saveProductFromDto(productFormDto);
            String successMsg = productFormDto.getId() != null ?
                    "Product '" + productFormDto.getName() + "' updated successfully with weight & volume specs!" :
                    "Product '" + productFormDto.getName() + "' created successfully with weight & volume specs!";
            redirectAttributes.addFlashAttribute("productSuccess", successMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("productFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("productFormDto", productFormDto);
        }

        return "redirect:/admin?tab=products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productSuccess", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("productError", "Failed to delete product: " + e.getMessage());
        }
        return "redirect:/admin?tab=products";
    }

    @PostMapping("/products/trigger-requisition")
    public String triggerSupplierRequisition(@RequestParam("productId") Long productId,
                                             @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                                             RedirectAttributes redirectAttributes) {
        try {
            String resultMsg = procurementEmailService.sendSupplierPurchaseRequisition(productId, quantity);
            redirectAttributes.addFlashAttribute("requisitionSuccess", resultMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("requisitionError", "Failed to send email requisition: " + e.getMessage());
        }
        return "redirect:/admin?tab=analytics";
    }

    @PostMapping("/products/restock")
    public String restockProduct(@RequestParam("productId") Long productId,
                                 @RequestParam("restockQuantity") BigDecimal restockQuantity,
                                 RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.restockProduct(productId, restockQuantity);
            redirectAttributes.addFlashAttribute("restockSuccess", "Product '" + product.getName() + "' restocked with +" + restockQuantity + " " + product.getUnit() + "! New stock: " + product.getStockQuantity());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("restockError", "Failed to restock product: " + e.getMessage());
        }
        return "redirect:/admin?tab=products";
    }

    // ---------------------------------------------------------
    // TAB C: FLEET ROSTER & DISPATCH LOGISTICS (PIN VERIFICATION)
    // ---------------------------------------------------------
    @PostMapping("/fleet/save")
    public String saveFleetVehicle(@Valid @ModelAttribute("fleetVehicleFormDto") FleetVehicleFormDto fleetVehicleFormDto,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.fleetVehicleFormDto", bindingResult);
            redirectAttributes.addFlashAttribute("fleetVehicleFormDto", fleetVehicleFormDto);
            redirectAttributes.addFlashAttribute("fleetFormError", "Please fix vehicle form validation errors.");
            return "redirect:/admin?tab=fleet";
        }

        try {
            fleetService.saveVehicleFromDto(fleetVehicleFormDto);
            redirectAttributes.addFlashAttribute("fleetSuccess", "Fleet vehicle saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fleetFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("fleetVehicleFormDto", fleetVehicleFormDto);
        }

        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/fleet/delete/{id}")
    public String deleteFleetVehicle(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            fleetService.deleteVehicle(id);
            redirectAttributes.addFlashAttribute("fleetSuccess", "Vehicle deleted from roster!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fleetError", "Failed to delete vehicle: " + e.getMessage());
        }
        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/drivers/save")
    public String saveDriver(@Valid @ModelAttribute("driverFormDto") DriverFormDto driverFormDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.driverFormDto", bindingResult);
            redirectAttributes.addFlashAttribute("driverFormDto", driverFormDto);
            redirectAttributes.addFlashAttribute("driverFormError", "Please fix driver form validation errors.");
            return "redirect:/admin?tab=fleet";
        }

        try {
            driverService.saveDriverFromDto(driverFormDto);
            redirectAttributes.addFlashAttribute("driverSuccess", "Driver details saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("driverFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("driverFormDto", driverFormDto);
        }

        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/drivers/delete/{id}")
    public String deleteDriver(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            driverService.deleteDriver(id);
            redirectAttributes.addFlashAttribute("driverSuccess", "Driver removed from roster!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("driverError", "Failed to delete driver: " + e.getMessage());
        }
        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/logistics/assign-dispatch")
    public String assignVehicleAndDriver(@RequestParam("orderId") Long orderId,
                                         @RequestParam("vehicleId") Long vehicleId,
                                         @RequestParam(value = "driverId", required = false) Long driverId,
                                         @RequestParam(value = "driverName", required = false) String driverName,
                                         RedirectAttributes redirectAttributes) {
        try {
            Order order = logisticsService.assignVehicleAndDriverToOrder(orderId, vehicleId, driverId, driverName);
            redirectAttributes.addFlashAttribute("dispatchSuccess", "Order #" + order.getOrderNumber() + " assigned to vehicle " + order.getAssignedVehicle().getLicensePlate() + " and dispatched!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("dispatchError", "Failed to assign vehicle: " + e.getMessage());
        }
        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/logistics/complete-delivery")
    public String completeOrderDelivery(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = logisticsService.completeOrderDelivery(orderId);
            redirectAttributes.addFlashAttribute("dispatchSuccess", "SUCCESS! Order #" + order.getOrderNumber() + " marked DELIVERED & PAID and vehicle " + (order.getAssignedVehicle() != null ? order.getAssignedVehicle().getLicensePlate() : "") + " released back to AVAILABLE roster!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("dispatchError", "Failed to complete delivery: " + e.getMessage());
        }
        return "redirect:/admin?tab=fleet";
    }

    @PostMapping("/logistics/verify-pin")
    public String verifyOrderPin(@RequestParam(value = "orderId", required = false) Long orderId,
                                 @RequestParam(value = "pin", required = false) String pin,
                                 @RequestParam(value = "pickupPin", required = false) String pickupPin,
                                 RedirectAttributes redirectAttributes) {
        try {
            String enteredPin = (pin != null && !pin.isBlank()) ? pin : pickupPin;
            Order order;
            if (orderId != null) {
                order = logisticsService.verifyOrderPinAndCompleteDelivery(orderId, enteredPin);
            } else {
                order = logisticsService.verifyPinAndCompleteDelivery(enteredPin);
            }
            redirectAttributes.addFlashAttribute("pinSuccess", "SUCCESS! Order Verification PIN (" + enteredPin.trim() + ") verified for Order #" + order.getOrderNumber() + ". Order status updated to DELIVERED & PAID and Vehicle " + (order.getAssignedVehicle() != null ? order.getAssignedVehicle().getLicensePlate() : "") + " released to AVAILABLE status!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("pinError", "PIN Verification Failed: " + e.getMessage());
        }
        return "redirect:/admin?tab=fleet";
    }

    // ---------------------------------------------------------
    // TAB D: SUPPLIER DIRECTORY CRUD
    // ---------------------------------------------------------
    @PostMapping("/suppliers/save")
    public String saveSupplier(@Valid @ModelAttribute("supplierFormDto") SupplierFormDto supplierFormDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Please fix supplier form errors." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.supplierFormDto", bindingResult);
            redirectAttributes.addFlashAttribute("supplierFormDto", supplierFormDto);
            redirectAttributes.addFlashAttribute("supplierFormError", errorMsg);
            return "redirect:/admin?tab=suppliers";
        }

        try {
            supplierService.saveSupplierFromDto(supplierFormDto);
            String successMsg = supplierFormDto.getId() != null ?
                    "Supplier '" + supplierFormDto.getCompanyName() + "' profile updated successfully!" :
                    "New supplier profile for '" + supplierFormDto.getCompanyName() + "' registered successfully!";
            redirectAttributes.addFlashAttribute("supplierSuccess", successMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("supplierFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("supplierFormDto", supplierFormDto);
        }

        return "redirect:/admin?tab=suppliers";
    }

    @PostMapping("/suppliers/delete/{id}")
    public String deleteSupplier(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("supplierSuccess", "Supplier profile deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("supplierError", "Failed to delete supplier: " + e.getMessage());
        }
        return "redirect:/admin?tab=suppliers";
    }

    // ---------------------------------------------------------
    // TAB E: STAFF USERS & ACCOUNT CONTROL CRUD
    // ---------------------------------------------------------
    @PostMapping("/users/save")
    public String saveUserByAdmin(@Valid @ModelAttribute("userAdminDto") UserAdminDto userAdminDto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().isEmpty() ? "Please correct the errors in the staff account form." : bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userAdminDto", bindingResult);
            redirectAttributes.addFlashAttribute("userAdminDto", userAdminDto);
            redirectAttributes.addFlashAttribute("userFormError", errorMsg);
            return "redirect:/admin?tab=users";
        }

        try {
            userService.saveOrUpdateUserByAdmin(userAdminDto);
            String successMsg = userAdminDto.getId() != null ?
                    "Account for '" + userAdminDto.getUsername() + "' updated successfully!" :
                    "Staff account '" + userAdminDto.getUsername() + "' created with role " + userAdminDto.getRole() + "!";
            redirectAttributes.addFlashAttribute("userSuccess", successMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("userFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("userAdminDto", userAdminDto);
        }

        return "redirect:/admin?tab=users";
    }

    @PostMapping("/users/register-staff")
    public String registerStaffUserLegacy(@Valid @ModelAttribute("registrationDto") UserRegistrationDto registrationDto,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registrationDto", bindingResult);
            redirectAttributes.addFlashAttribute("registrationDto", registrationDto);
            redirectAttributes.addFlashAttribute("userFormError", "Please fix staff account form errors.");
            return "redirect:/admin?tab=users";
        }

        try {
            userService.registerUser(registrationDto);
            redirectAttributes.addFlashAttribute("userSuccess", "Staff account '" + registrationDto.getUsername() + "' created with role " + registrationDto.getRole() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("userFormError", e.getMessage());
            redirectAttributes.addFlashAttribute("registrationDto", registrationDto);
        }

        return "redirect:/admin?tab=users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUserByAdmin(@PathVariable("id") Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            String currentAdminUsername = authentication != null ? authentication.getName() : "";
            userService.deleteUserByAdmin(id, currentAdminUsername);
            redirectAttributes.addFlashAttribute("userSuccess", "User account deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("userError", "Failed to delete user account: " + e.getMessage());
        }
        return "redirect:/admin?tab=users";
    }

    @GetMapping("/revenue/download")
    public ResponseEntity<byte[]> downloadRevenueReport(@RequestParam(value = "year", required = false) Integer yearParam,
                                                        @RequestParam(value = "month", required = false) Integer monthParam) {
        LocalDate now = LocalDate.now();
        int selectedYear = (yearParam != null && yearParam >= 2000 && yearParam <= 2100) ? yearParam : now.getYear();
        int selectedMonth = (monthParam != null && monthParam >= 1 && monthParam <= 12) ? monthParam : now.getMonthValue();
        String selectedMonthName = java.time.Month.of(selectedMonth).name();

        BigDecimal hardwareAllTime = orderService.getHardwareRevenue();
        BigDecimal deliveryAllTime = orderService.getDeliveryRevenue();
        BigDecimal totalAllTime = orderService.getTotalRevenue();

        BigDecimal hardwareMonthly = orderService.getMonthlyHardwareRevenue(selectedYear, selectedMonth);
        BigDecimal deliveryMonthly = orderService.getMonthlyDeliveryRevenue(selectedYear, selectedMonth);
        BigDecimal totalMonthly = orderService.getMonthlyTotalRevenue(selectedYear, selectedMonth);

        BigDecimal hardwareYearly = orderService.getYearlyHardwareRevenue(selectedYear);
        BigDecimal yearlyDelivery = orderService.getYearlyDeliveryRevenue(selectedYear);
        BigDecimal yearlyTotal = orderService.getYearlyTotalRevenue(selectedYear);

        StringBuilder csv = new StringBuilder();
        csv.append("Revenue Category,Monthly (").append(selectedMonthName).append(" ").append(selectedYear).append("),Yearly (").append(selectedYear).append("),All-Time Total (LKR)\n");
        csv.append("Hardware Sales Revenue (LKR),").append(hardwareMonthly).append(",").append(hardwareYearly).append(",").append(hardwareAllTime).append("\n");
        csv.append("Logistics Freight Revenue (LKR),").append(deliveryMonthly).append(",").append(yearlyDelivery).append(",").append(deliveryAllTime).append("\n");
        csv.append("Total Gross Revenue (LKR),").append(totalMonthly).append(",").append(yearlyTotal).append(",").append(totalAllTime).append("\n");

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", String.format("Financial_Revenue_Report_%d_%02d.csv", selectedYear, selectedMonth));

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}
