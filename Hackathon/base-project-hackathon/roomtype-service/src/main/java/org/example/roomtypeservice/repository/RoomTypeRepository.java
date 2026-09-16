package org.example.roomtypeservice.repository;

import org.example.roomtypeservice.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
}
