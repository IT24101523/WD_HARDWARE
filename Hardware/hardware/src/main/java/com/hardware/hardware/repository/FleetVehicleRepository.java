package com.hardware.hardware.repository;

import com.hardware.hardware.model.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Long> {
    List<FleetVehicle> findByStatus(String status);
    List<FleetVehicle> findByVehicleTypeAndStatus(String vehicleType, String status);
    Optional<FleetVehicle> findByLicensePlate(String licensePlate);
    boolean existsByLicensePlate(String licensePlate);
}
