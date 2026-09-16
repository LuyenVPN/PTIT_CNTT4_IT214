package org.example.roomtypeservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_types")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomType {
    @Id
    @Column(name = "room_type_id", length = 15)
    private String roomTypeId;

    @Column(name = "room_type_name", length = 100, nullable = false, unique = true)
    private String roomTypeName;

    @Column(name = "description", length = 255)
    private String description;  

    @Column(name = "price", precision = 10, scale = 2)
    private Double price;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy;
}
