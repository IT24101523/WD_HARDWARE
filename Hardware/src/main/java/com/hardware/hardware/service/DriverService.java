package com.hardware.hardware.service;

import com.hardware.hardware.dto.DriverFormDto;
import com.hardware.hardware.model.Driver;
import com.hardware.hardware.model.FleetVehicle;
import com.hardware.hardware.repository.DriverRepository;
import com.hardware.hardware.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final FleetVehicleRepository fleetVehicleRepository;

    public List<Driver> getAllDrivers() {
        return driverRepository.findAllByOrderByFullNameAsc();
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByStatusOrderByFullNameAsc("AVAILABLE");
    }

    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }

    @Transactional
    public Driver saveDriverFromDto(DriverFormDto dto) {
        Driver driver;
        if (dto.getId() != null) {
            if (driverRepository.existsByLicenseNumberAndIdNot(dto.getLicenseNumber().trim(), dto.getId())) {
                throw new IllegalArgumentException("License number '" + dto.getLicenseNumber().trim() + "' is already registered to another driver.");
            }
            driver = driverRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + dto.getId()));
        } else {
            if (driverRepository.existsByLicenseNumber(dto.getLicenseNumber().trim())) {
                throw new IllegalArgumentException("License number '" + dto.getLicenseNumber().trim() + "' is already registered.");
            }
            driver = new Driver();
        }

        driver.setFullName(dto.getFullName().trim());
        driver.setPhone(dto.getPhone().trim());
        driver.setLicenseNumber(dto.getLicenseNumber().trim().toUpperCase());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            driver.setStatus(dto.getStatus().trim().toUpperCase());
        } else if (driver.getStatus() == null) {
            driver.setStatus("AVAILABLE");
        }

        if (dto.getAssignedVehicleId() != null) {
            FleetVehicle vehicle = fleetVehicleRepository.findById(dto.getAssignedVehicleId()).orElse(null);
            driver.setAssignedVehicle(vehicle);
        } else {
            driver.setAssignedVehicle(null);
        }

        return driverRepository.save(driver);
    }

    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + id));

        if ("ON_DELIVERY".equalsIgnoreCase(driver.getStatus())) {
            throw new IllegalStateException("Cannot delete driver '" + driver.getFullName() + "' while currently assigned to an active delivery!");
        }

        driverRepository.delete(driver);
    }
}
