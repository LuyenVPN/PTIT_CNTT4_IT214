package org.example.userservice.service;
import jakarta.servlet.http.HttpServletRequest;
import org.example.userservice.model.entity.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public Optional<User> findById(Long id) {

        return userRepository.findById(id);
    }

    public void initializeUsers() {
        if (userRepository.count() == 0) {
            User user = User
                    .builder()
                    .username("huongcaoha")
                    .password("12345678")
                    .role("USER")
                    .status("ACTIVE")
                    .email("huongcaoha1994@gmal.com")
                    .build();
            userRepository.save(user);
        }
    }

}
