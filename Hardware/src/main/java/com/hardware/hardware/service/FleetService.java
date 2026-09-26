package com.hardware.hardware.service;

import com.hardware.hardware.dto.FleetVehicleFormDto;
import com.hardware.hardware.model.FleetVehicle;
import com.hardware.hardware.repository.FleetVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final FleetVehicleRepository fleetVehicleRepository;

    public List<FleetVehicle> getAllVehicles() {
        return fleetVehicleRepository.findAll();
    }

    public Optional<FleetVehicle> getVehicleById(Long id) {
        return fleetVehicleRepository.findById(id);
    }

    @Transactional
    public FleetVehicle saveVehicleFromDto(FleetVehicleFormDto dto) {
        String plate = dto.getLicensePlate().toUpperCase().trim();
        FleetVehicle vehicle;
        if (dto.getId() != null) {
            vehicle = fleetVehicleRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + dto.getId()));

            Optional<FleetVehicle> existingWithPlate = fleetVehicleRepository.findByLicensePlate(plate);
            if (existingWithPlate.isPresent() && !existingWithPlate.get().getId().equals(dto.getId())) {
                throw new IllegalArgumentException("A vehicle with license plate '" + plate + "' already exists!");
            }
        } else {
            if (fleetVehicleRepository.existsByLicensePlate(plate)) {
                throw new IllegalArgumentException("A vehicle with license plate '" + plate + "' already exists!");
            }
            vehicle = new FleetVehicle();
            vehicle.setStatus("AVAILABLE");
        }

        String model = dto.getModelName() != null ? dto.getModelName().trim() : "Motorbike";
        String type;
        if (model.equalsIgnoreCase("Motorbike") || model.equalsIgnoreCase("BIKE")) {
            type = "BIKE";
            model = "Motorbike";
        } else if (model.equalsIgnoreCase("Diesel Three Wheel") || model.equalsIgnoreCase("TUKTUK")) {
            type = "TUKTUK";
            model = "Diesel Three Wheel";
        } else if (model.equalsIgnoreCase("Dimo Batta") || model.equalsIgnoreCase("LIGHT_TRUCK")) {
            type = "LIGHT_TRUCK";
            model = "Dimo Batta";
        } else if (model.equalsIgnoreCase("Mini Lorry") || model.equalsIgnoreCase("MINI_LORRY")) {
            type = "MINI_LORRY";
            model = "Mini Lorry";
        } else if (model.equalsIgnoreCase("Dump Truck") || model.equalsIgnoreCase("TIPPER")) {
            type = "TIPPER";
            model = "Dump Truck";
        } else {
            type = (dto.getVehicleType() != null && !dto.getVehicleType().isBlank()) ? dto.getVehicleType().toUpperCase().trim() : "LIGHT_TRUCK";
        }

        vehicle.setVehicleType(type);
        vehicle.setModelName(model);
        vehicle.setLicensePlate(plate);
        vehicle.setMaxWeightKg(dto.getMaxWeightKg());
        vehicle.setMaxVolumeKubs(dto.getMaxVolumeKubs());
        vehicle.setBaseFee(dto.getBaseFee());
        vehicle.setPerKmRate(dto.getPerKmRate());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            vehicle.setStatus(dto.getStatus().toUpperCase().trim());
        } else if (vehicle.getStatus() == null) {
            vehicle.setStatus("AVAILABLE");
        }

        return fleetVehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        FleetVehicle vehicle = fleetVehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));

        if ("DELIVERING".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Cannot delete vehicle '" + vehicle.getLicensePlate() + "' because it is currently assigned to an active delivery!");
        }

        fleetVehicleRepository.delete(vehicle);
    }
}
