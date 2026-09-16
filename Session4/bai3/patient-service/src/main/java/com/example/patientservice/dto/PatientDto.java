<<<<<<< HEAD
package com.example.patientservice.dto;

import com.example.patientservice.model.Patient;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDto {

    private String fullName;

    private LocalDate dateOfBirth;

    private Patient.Gender gender;

    private String phone;

    private String address;

    private String insuranceId;
=======
package com.example.patientservice.dto;

import com.example.patientservice.model.Patient;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDto {

    private String fullName;

    private LocalDate dateOfBirth;

    private Patient.Gender gender;

    private String phone;

    private String address;

    private String insuranceId;
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
}