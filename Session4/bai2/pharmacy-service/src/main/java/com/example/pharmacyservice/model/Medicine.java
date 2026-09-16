<<<<<<< HEAD
package com.example.pharmacyservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String activeIngredient;

    @Column(length = 50)
    private String unit;

    @Column(length = 150)
    private String manufacturer;

    private BigDecimal price;
=======
package com.example.pharmacyservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String activeIngredient;

    @Column(length = 50)
    private String unit;

    @Column(length = 150)
    private String manufacturer;

    private BigDecimal price;
>>>>>>> 4101676851925098dd510ffc36d26f9bc968f423
}