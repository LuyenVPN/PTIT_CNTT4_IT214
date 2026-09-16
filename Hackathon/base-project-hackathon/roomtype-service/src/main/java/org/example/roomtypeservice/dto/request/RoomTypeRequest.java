package org.example.roomtypeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomTypeRequest {
    @NotBlank(message = "Không được để trống roomtype id")
    private String roomTypeId;
    @NotBlank(message = "Không được để trống roomtype name")
    private String roomTypeName;
    private String description;
    private Double price;
    private Integer maxOccupancy;
}
