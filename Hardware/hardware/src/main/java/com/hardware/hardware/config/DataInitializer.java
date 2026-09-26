package com.hardware.hardware.config;

import com.hardware.hardware.model.*;
import com.hardware.hardware.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureStaffUser("admin", "admin@hardware.com", "admin123", "Super Admin", "+94 77 123 4567", "Main Hardware Hub, Colombo", "ROLE_ADMIN");
        ensureStaffUser("inventory", "inventory@hardware.com", "admin123", "Inventory Control Officer", "+94 77 888 1122", "Stock Yard, Colombo", "ROLE_INVENTORY_OFFICER");
        ensureStaffUser("fleet", "fleet@hardware.com", "admin123", "Fleet Logistics Officer", "+94 77 999 3344", "Dispatch Yard, Colombo", "ROLE_FLEET_OFFICER");
        
        ensureCustomer("customer", "customer@hardware.com", "customer123", "John Silva", "+94 71 987 6543", "123 Temple Road, Nugegoda", "ROLE_CUSTOMER");
        ensureCustomer("kamal", "kamal@hardware.com", "kamal123", "Kamal Perera", "+94 76 555 1234", "45 Main Street, Galle", "ROLE_CUSTOMER");

        ensureDriver("Sunil Perera", "+94 77 111 2233", "DL-8890123");
        ensureDriver("Bandula Jayasinghe", "+94 71 444 5566", "DL-9901234");
        ensureDriver("Saman Silva", "+94 76 777 8899", "DL-7712345");

        // Update requested product image URLs in database
        updateProductImageUrl(1L, "Portland Cement 50kg Bag", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ4EGi7G1s3kROAgV0eBEB5ySpX1IBj7ff9gJPdL4qPtYFF2nTUlHiVGYU&s=10");
        updateProductImageUrl(2L, "Screened River Sand", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQieIPby7i0ROn67HV-K-XOybXY3DR3YIXQOwgL8K7bfAtuO82MLhDHBDy1&s=10");
        updateProductImageUrl(3L, "Aggregate Metal Chips (3/4\")", "https://4.imimg.com/data4/VK/YS/IMOB-41589443/22829601_698732786987244_9108158046707123231_o.jpg");
        updateProductImageUrl(7L, "Digital Multimeter & Circuit Tester", "https://img.drz.lazcdn.com/static/bd/p/c5a4025c059c780c76d9dba4795156bd.png_960x960q80.png_.webp");
        updateProductImageUrl(8L, "PVC Water Pipe (1 inch, 4m)", "https://tiimg.tistatic.com/fp/1/008/434/round-shape-seamless-1-mm-thickness-4-inch-length-pvc-water-pipe-055.jpg");
        updateProductImageUrl(11L, "Dulux WeatherShield Exterior Paint (White 10L)", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRul6x2f10obel1vzsx-yWn0ywhIIwDo79qsGQ7qPcQ9g&s=10");
        updateProductImageUrl(12L, "S-Lon PVC Elbow Connector (1 inch)", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9Bi5gRRtVoU0GB8l465ZstvSG_j0Wjm2xgbl_xHIuaA&s=10");
        updateProductImageUrl(13L, "Torpedo Magnetic Spirit Level 12-Inch", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTM4BdyBuzlCAyZi2ShyDhGLy1FBzwRLyWuJDNBJtlRKw&s=10");
        updateProductImageUrl(14L, "Steel Toe Safety Boots (Size 42)", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSdQ9eUanKwLUxanWu_H3sx1oJC76I3REEwIm-VMSnR7w&s=10");
        updateProductImageUrl(15L, "Single Pole MCB Circuit Breaker 16A", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTjdeDSrdqGrrkdLihNXdiaVYtP_69K13nRzUeCdR7kwg&s=10");
        updateProductImageUrl(16L, "Nippon Red Oxide Metal Primer (4L)", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT-6FImVx8K-ujBrsbCUdH5evqvvxog7MT7LLhV0f1w1w&s=10");
        updateProductImageUrl(17L, "Heavy Duty Construction Wheelbarrow 90L", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTW2cZop6vKnIs4W1Ibr7ohLsnuN_gaWCJ0rjv5F5OgGg&s=10");
        updateProductImageUrl(18L, "Makita Circular Saw 1800W 7-1/4\"", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRC_7og4f0NAatQ2eVZBlNYfPVsnXrN6V6eYABeHxRIrg&s=10");
        updateProductImageUrl(19L, "Adjustable Plumbing Pipe Wrench 14-Inch", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ37FQBrGmC_Mh7GKIeVD81_Gw04UlEuvJixV2wdzrU9w&s");
        updateProductImageUrl(20L, "High-Yield Deformed Steel Rebar 12mm (6m)", "https://www.bmsteel.co.uk/images/products/standard/318_16052.jpg");
        updateProductImageUrl(21L, "Outdoor Waterproof LED Flood Light 100W", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSZvFMAsKCziuCxEv1Z1iGQozRlcIOvvDC9rSeYYRBYwg&s=10");
    }

    private void updateProductImageUrl(Long productId, String fallbackName, String newImageUrl) {
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isEmpty()) {
            pOpt = productRepository.findByName(fallbackName);
        }
        pOpt.ifPresent(product -> {
            product.setImageUrl(newImageUrl);
            productRepository.save(product);
        });
    }

    private void ensureDriver(String fullName, String phone, String licenseNumber) {
        if (!driverRepository.existsByLicenseNumber(licenseNumber)) {
            Driver driver = Driver.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .licenseNumber(licenseNumber)
                    .status("AVAILABLE")
                    .build();
            driverRepository.save(driver);
        }
    }

    private void ensureStaffUser(String username, String email, String rawPassword, String fullName, String phone, String address, String role) {
        staffUserRepository.findByUsername(username).ifPresentOrElse(
            user -> {
                if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    staffUserRepository.save(user);
                }
            },
            () -> {
                StaffUser user = StaffUser.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName(fullName)
                        .phone(phone)
                        .address(address)
                        .role(role)
                        .build();
                staffUserRepository.save(user);
            }
        );
    }

    private void ensureCustomer(String username, String email, String rawPassword, String fullName, String phone, String address, String role) {
        customerRepository.findByUsername(username).ifPresentOrElse(
            user -> {
                if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setRole(role);
                    customerRepository.save(user);
                }
            },
            () -> {
                Customer user = Customer.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName(fullName)
                        .phone(phone)
                        .address(address)
                        .role(role)
                        .build();
                customerRepository.save(user);
            }
        );
    }
}
