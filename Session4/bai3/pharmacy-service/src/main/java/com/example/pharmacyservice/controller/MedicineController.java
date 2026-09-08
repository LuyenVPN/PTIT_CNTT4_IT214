package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.dto.MedicineDto;
import com.example.pharmacyservice.model.Medicine;
import com.example.pharmacyservice.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @PostMapping
    public ResponseEntity<Medicine> create(
            @RequestBody MedicineDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(medicineService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getAll() {

        return ResponseEntity.ok(
                medicineService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                medicineService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> update(
            @PathVariable Long id,
            @RequestBody MedicineDto dto) {

        return ResponseEntity.ok(
                medicineService.update(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        medicineService.delete(id);

        return ResponseEntity.noContent().build();
    }
}