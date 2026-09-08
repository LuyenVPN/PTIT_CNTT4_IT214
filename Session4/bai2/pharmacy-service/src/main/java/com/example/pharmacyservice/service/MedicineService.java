package com.example.pharmacyservice.service;


import com.example.pharmacyservice.dto.MedicineDto;
import com.example.pharmacyservice.model.Medicine;
import com.example.pharmacyservice.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public Medicine create(MedicineDto dto) {

        Medicine medicine = Medicine.builder()
                .name(dto.getName())
                .activeIngredient(dto.getActiveIngredient())
                .unit(dto.getUnit())
                .manufacturer(dto.getManufacturer())
                .price(dto.getPrice())
                .build();

        return medicineRepository.save(medicine);
    }

    public List<Medicine> getAll() {
        return medicineRepository.findAll();
    }

    public Medicine getById(Long id) {

        return medicineRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Medicine not found with id: " + id
                        ));
    }

    public Medicine update(Long id, MedicineDto dto) {

        Medicine medicine = getById(id);

        medicine.setName(dto.getName());
        medicine.setActiveIngredient(dto.getActiveIngredient());
        medicine.setUnit(dto.getUnit());
        medicine.setManufacturer(dto.getManufacturer());
        medicine.setPrice(dto.getPrice());

        return medicineRepository.save(medicine);
    }

    public void delete(Long id) {

        Medicine medicine = getById(id);

        medicineRepository.delete(medicine);
    }
}
