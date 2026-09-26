package com.hardware.hardware.service;

import com.hardware.hardware.dto.SupplierFormDto;
import com.hardware.hardware.model.Product;
import com.hardware.hardware.model.Supplier;
import com.hardware.hardware.repository.ProductRepository;
import com.hardware.hardware.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCompanyNameAsc();
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    @Transactional
    public Supplier saveSupplierFromDto(SupplierFormDto dto) {
        Supplier supplier;
        if (dto.getId() != null) {
            if (supplierRepository.existsByCompanyNameAndIdNot(dto.getCompanyName().trim(), dto.getId())) {
                throw new IllegalArgumentException("Supplier with company name '" + dto.getCompanyName().trim() + "' already exists.");
            }
            supplier = supplierRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + dto.getId()));
        } else {
            if (supplierRepository.existsByCompanyName(dto.getCompanyName().trim())) {
                throw new IllegalArgumentException("Supplier with company name '" + dto.getCompanyName().trim() + "' already exists.");
            }
            supplier = new Supplier();
        }

        supplier.setCompanyName(dto.getCompanyName().trim());
        supplier.setContactPerson(dto.getContactPerson().trim());
        supplier.setEmail(dto.getEmail().trim().toLowerCase());
        supplier.setPhone(dto.getPhone().trim());
        supplier.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));

        // Disassociate products linked to this supplier so foreign key doesn't fail
        List<Product> products = productRepository.findBySupplierId(id);
        for (Product p : products) {
            p.setSupplier(null);
            productRepository.save(p);
        }

        supplierRepository.delete(supplier);
    }
}
