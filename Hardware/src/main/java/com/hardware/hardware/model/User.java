package com.hardware.hardware.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false, unique = true, length = 100)
    protected String username;

    @Column(nullable = false, unique = true, length = 150)
    protected String email;

    @Column(nullable = false)
    protected String password;

    @Column(name = "full_name", nullable = false, length = 150)
    protected String fullName;

    @Column(length = 20)
    protected String phone;

    @Column(columnDefinition = "TEXT")
    protected String address;

    @Column(nullable = false, length = 50)
    protected String role; // 'ROLE_CUSTOMER', 'ROLE_ADMIN', 'ROLE_INVENTORY_OFFICER', 'ROLE_FLEET_OFFICER'

    @Column(name = "created_at", insertable = false, updatable = false)
    protected LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
