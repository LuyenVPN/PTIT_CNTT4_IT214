package org.example.roomservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_name", length = 70)
    private String roomName;

    @Column(name = "room_type_id", length = 15)
    private String roomTypeId;

    @Column(name = "status")
    private Boolean status;
}
