package org.example.roomservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.example.roomtypeservice.dto.response.RoomTypeResponse;

@FeignClient(name = "room-type-service")
public interface RoomTypeServiceCall {
//    @GetMapping("/api/v1/room-types/{roomTypeId}")
//    ResponseEntity<APIResponse<RoomTypeResponse>> getById(@PathVariable String roomTypeId);
}