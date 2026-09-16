package org.example.roomtypeservice.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomTypeResponse {
    private String roomTypeId;
    private String roomTypeName;
    private String description;
    private double price;
    private int maxOccupancy;
}
