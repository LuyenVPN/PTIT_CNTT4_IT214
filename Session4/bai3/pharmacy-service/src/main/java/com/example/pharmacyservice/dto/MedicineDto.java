package com.example.pharmacyservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedicineDto {

    private String name;

    private String activeIngredient;

    private String unit;

    private String manufacturer;

    private BigDecimal price;
}
