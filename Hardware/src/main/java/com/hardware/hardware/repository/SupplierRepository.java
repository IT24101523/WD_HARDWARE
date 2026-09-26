package com.hardware.hardware.repository;

import com.hardware.hardware.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCompanyName(String companyName);
    boolean existsByCompanyName(String companyName);
    boolean existsByCompanyNameAndIdNot(String companyName, Long id);
    List<Supplier> findAllByOrderByCompanyNameAsc();
}

