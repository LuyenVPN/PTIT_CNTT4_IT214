package org.example.userservice.model.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String status; // "ACTIVE", "INACTIVE", "LOCKED"
    private String role;
    private String email;
    private String password;
}
