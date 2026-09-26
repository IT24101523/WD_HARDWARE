package com.hardware.hardware.service;

import com.hardware.hardware.model.Customer;
import com.hardware.hardware.model.StaffUser;
import com.hardware.hardware.model.User;
import com.hardware.hardware.repository.CustomerRepository;
import com.hardware.hardware.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // First search in staff_users table
        Optional<StaffUser> staffOpt = staffUserRepository.findByUsername(usernameOrEmail);
        if (staffOpt.isEmpty()) {
            staffOpt = staffUserRepository.findByEmail(usernameOrEmail);
        }

        User user = null;
        if (staffOpt.isPresent()) {
            user = staffOpt.get();
        } else {
            // Search in customers table
            Optional<Customer> custOpt = customerRepository.findByUsername(usernameOrEmail);
            if (custOpt.isEmpty()) {
                custOpt = customerRepository.findByEmail(usernameOrEmail);
            }
            if (custOpt.isPresent()) {
                user = custOpt.get();
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail);
        }

        String role = user.getRole();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
