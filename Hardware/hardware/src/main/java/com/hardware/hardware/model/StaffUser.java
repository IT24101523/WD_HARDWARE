package com.hardware.hardware.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "staff_users")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StaffUser extends User {

}
