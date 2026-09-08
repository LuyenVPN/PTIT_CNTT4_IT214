package com.example.doctorservice.service;

import com.example.doctorservice.dto.DoctorDto;
import com.example.doctorservice.model.Doctor;
import com.example.doctorservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public Doctor create(DoctorDto dto) {

        Doctor doctor = Doctor.builder()
                .fullName(dto.getFullName())
                .specialty(dto.getSpecialty())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .build();

        return doctorRepository.save(doctor);
    }

    public List<Doctor> getAll() {
        return doctorRepository.findAll();
    }

    public Doctor getById(Long id) {

        return doctorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found with id: " + id));
    }

    public Doctor update(Long id, DoctorDto dto) {

        Doctor doctor = getById(id);

        doctor.setFullName(dto.getFullName());
        doctor.setSpecialty(dto.getSpecialty());
        doctor.setPhone(dto.getPhone());
        doctor.setEmail(dto.getEmail());

        return doctorRepository.save(doctor);
    }

    public void delete(Long id) {

        Doctor doctor = getById(id);

        doctorRepository.delete(doctor);
    }
}