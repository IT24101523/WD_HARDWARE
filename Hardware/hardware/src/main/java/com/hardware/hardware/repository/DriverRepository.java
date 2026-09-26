package com.hardware.hardware.repository;

import com.hardware.hardware.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatusOrderByFullNameAsc(String status);
    List<Driver> findAllByOrderByFullNameAsc();
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
}
