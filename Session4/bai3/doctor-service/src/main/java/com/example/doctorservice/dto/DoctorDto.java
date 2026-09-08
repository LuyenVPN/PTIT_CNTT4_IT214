package com.example.doctorservice.dto;


import lombok.Data;

@Data
public class DoctorDto {

    private String fullName;

    private String specialty;

    private String phone;

    private String email;
}