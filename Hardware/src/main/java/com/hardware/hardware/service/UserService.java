package com.hardware.hardware.service;

import com.hardware.hardware.dto.PasswordUpdateDto;
import com.hardware.hardware.dto.ProfileUpdateDto;
import com.hardware.hardware.dto.UserAdminDto;
import com.hardware.hardware.dto.UserRegistrationDto;
import com.hardware.hardware.model.Customer;
import com.hardware.hardware.model.StaffUser;
import com.hardware.hardware.model.User;
import com.hardware.hardware.repository.CartItemRepository;
import com.hardware.hardware.repository.CustomerRepository;
import com.hardware.hardware.repository.OrderRepository;
import com.hardware.hardware.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(staffUserRepository.findAllByOrderByCreatedAtDesc());
        allUsers.addAll(customerRepository.findAllByOrderByCreatedAtDesc());
        return allUsers;
    }

    public List<User> getStaffUsers() {
        return new ArrayList<>(staffUserRepository.findAllByOrderByCreatedAtDesc());
    }

    public List<User> getCustomerUsers() {
        return new ArrayList<>(customerRepository.findAllByOrderByCreatedAtDesc());
    }

    public Optional<User> getUserById(Long id) {
        Optional<StaffUser> staffOpt = staffUserRepository.findById(id);
        if (staffOpt.isPresent()) {
            return Optional.of(staffOpt.get());
        }
        Optional<Customer> custOpt = customerRepository.findById(id);
        return custOpt.map(c -> c);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<StaffUser> getStaffUserById(Long id) {
        return staffUserRepository.findById(id);
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();

        if (staffUserRepository.existsByUsername(username) || customerRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (staffUserRepository.existsByEmail(email) || customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }

        String role = dto.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_CUSTOMER";
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }

        if ("ROLE_CUSTOMER".equalsIgnoreCase(role)) {
            Customer customer = Customer.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .fullName(dto.getFullName().trim())
                    .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                    .address(dto.getAddress() != null ? dto.getAddress().trim() : "")
                    .role("ROLE_CUSTOMER")
                    .build();
            return customerRepository.save(customer);
        } else {
            StaffUser staffUser = StaffUser.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .fullName(dto.getFullName().trim())
                    .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                    .address(dto.getAddress() != null ? dto.getAddress().trim() : "")
                    .role(role)
                    .build();
            return staffUserRepository.save(staffUser);
        }
    }

    @Transactional
    public User saveOrUpdateUserByAdmin(UserAdminDto dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();

        if (dto.getId() != null) {
            // Check if editing a StaffUser
            Optional<StaffUser> staffOpt = staffUserRepository.findById(dto.getId());
            if (staffOpt.isPresent()) {
                if (staffUserRepository.existsByUsernameAndIdNot(username, dto.getId()) || customerRepository.existsByUsername(username)) {
                    throw new IllegalArgumentException("Username '" + username + "' is already taken by another account.");
                }
                if (staffUserRepository.existsByEmailAndIdNot(email, dto.getId()) || customerRepository.existsByEmail(email)) {
                    throw new IllegalArgumentException("Email '" + email + "' is already registered to another account.");
                }
                StaffUser staff = staffOpt.get();
                staff.setUsername(username);
                staff.setEmail(email);
                staff.setFullName(dto.getFullName().trim());
                staff.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : "");
                staff.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
                if (dto.getRole() != null && !dto.getRole().isBlank()) {
                    String role = dto.getRole().startsWith("ROLE_") ? dto.getRole() : "ROLE_" + dto.getRole().toUpperCase();
                    staff.setRole(role);
                }
                if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                    if (dto.getPassword().trim().length() < 6) {
                        throw new IllegalArgumentException("Password must be at least 6 characters.");
                    }
                    staff.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
                }
                return staffUserRepository.save(staff);
            }

            // Check if editing a Customer
            Optional<Customer> custOpt = customerRepository.findById(dto.getId());
            if (custOpt.isPresent()) {
                if (customerRepository.existsByUsernameAndIdNot(username, dto.getId()) || staffUserRepository.existsByUsername(username)) {
                    throw new IllegalArgumentException("Username '" + username + "' is already taken by another account.");
                }
                if (customerRepository.existsByEmailAndIdNot(email, dto.getId()) || staffUserRepository.existsByEmail(email)) {
                    throw new IllegalArgumentException("Email '" + email + "' is already registered to another account.");
                }
                Customer cust = custOpt.get();
                cust.setUsername(username);
                cust.setEmail(email);
                cust.setFullName(dto.getFullName().trim());
                cust.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : "");
                cust.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
                if (dto.getRole() != null && !dto.getRole().isBlank()) {
                    String role = dto.getRole().startsWith("ROLE_") ? dto.getRole() : "ROLE_" + dto.getRole().toUpperCase();
                    cust.setRole(role);
                }
                if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                    if (dto.getPassword().trim().length() < 6) {
                        throw new IllegalArgumentException("Password must be at least 6 characters.");
                    }
                    cust.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
                }
                return customerRepository.save(cust);
            }

            throw new IllegalArgumentException("User not found with ID: " + dto.getId());
        } else {
            // Create new user
            if (staffUserRepository.existsByUsername(username) || customerRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username '" + username + "' is already taken.");
            }
            if (staffUserRepository.existsByEmail(email) || customerRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email '" + email + "' is already registered.");
            }
            if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
                throw new IllegalArgumentException("Password is required and must be at least 6 characters.");
            }

            String role = dto.getRole();
            if (role == null || role.isBlank()) {
                role = "ROLE_CUSTOMER";
            } else if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role.toUpperCase();
            }

            if ("ROLE_CUSTOMER".equalsIgnoreCase(role)) {
                Customer cust = Customer.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(dto.getPassword().trim()))
                        .fullName(dto.getFullName().trim())
                        .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                        .address(dto.getAddress() != null ? dto.getAddress().trim() : "")
                        .role("ROLE_CUSTOMER")
                        .build();
                return customerRepository.save(cust);
            } else {
                StaffUser staff = StaffUser.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(dto.getPassword().trim()))
                        .fullName(dto.getFullName().trim())
                        .phone(dto.getPhone() != null ? dto.getPhone().trim() : "")
                        .address(dto.getAddress() != null ? dto.getAddress().trim() : "")
                        .role(role)
                        .build();
                return staffUserRepository.save(staff);
            }
        }
    }

    @Transactional
    public void deleteUserByAdmin(Long id, String currentAdminUsername) {
        Optional<StaffUser> staffOpt = staffUserRepository.findById(id);
        if (staffOpt.isPresent()) {
            StaffUser staff = staffOpt.get();
            if (staff.getUsername().equalsIgnoreCase(currentAdminUsername)) {
                throw new IllegalArgumentException("Security Protection: You cannot delete your currently active administrator account!");
            }
            staffUserRepository.delete(staff);
            return;
        }

        Optional<Customer> custOpt = customerRepository.findById(id);
        if (custOpt.isPresent()) {
            Customer cust = custOpt.get();
            if (cust.getUsername().equalsIgnoreCase(currentAdminUsername)) {
                throw new IllegalArgumentException("Security Protection: You cannot delete your currently active account!");
            }
            long orderCount = orderRepository.countByUserId(id);
            if (orderCount > 0) {
                throw new IllegalArgumentException("Cannot delete customer '" + cust.getUsername() + "' because they have " + orderCount + " order records in the database.");
            }
            cartItemRepository.deleteByUserId(id);
            customerRepository.delete(cust);
            return;
        }

        throw new IllegalArgumentException("User not found with ID: " + id);
    }

    public Optional<User> findByUsername(String username) {
        Optional<StaffUser> staffOpt = staffUserRepository.findByUsername(username);
        if (staffOpt.isPresent()) {
            return Optional.of(staffOpt.get());
        }
        Optional<Customer> custOpt = customerRepository.findByUsername(username);
        return custOpt.map(c -> c);
    }

    public Optional<User> findByEmail(String email) {
        Optional<StaffUser> staffOpt = staffUserRepository.findByEmail(email);
        if (staffOpt.isPresent()) {
            return Optional.of(staffOpt.get());
        }
        Optional<Customer> custOpt = customerRepository.findByEmail(email);
        return custOpt.map(c -> c);
    }

    @Transactional
    public User updateUserProfile(User user, ProfileUpdateDto dto) {
        String newEmail = dto.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail)) {
            if (staffUserRepository.existsByEmail(newEmail) || customerRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already in use by another account.");
            }
        }
        user.setFullName(dto.getFullName().trim());
        user.setEmail(newEmail);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : "");
        user.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");

        if (user instanceof StaffUser staffUser) {
            return staffUserRepository.save(staffUser);
        } else if (user instanceof Customer customer) {
            return customerRepository.save(customer);
        }
        return user;
    }

    @Transactional
    public void updateUserPassword(User user, PasswordUpdateDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect. Please try again.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        if (user instanceof StaffUser staffUser) {
            staffUserRepository.save(staffUser);
        } else if (user instanceof Customer customer) {
            customerRepository.save(customer);
        }
    }
}
