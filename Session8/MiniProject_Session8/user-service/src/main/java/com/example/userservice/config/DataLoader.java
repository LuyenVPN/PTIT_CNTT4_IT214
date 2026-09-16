package com.example.userservice.config;

import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    User.builder()
                            .fullName("Nguyễn Văn An")
                            .email("an.nguyen@example.com")
                            .phoneNumber("0901234567")
                            .address("72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh")
                            .memberTier("VIP Gold")
                            .build(),
                    User.builder()
                            .fullName("Trần Thị Mai")
                            .email("mai.tran@example.com")
                            .phoneNumber("0912345678")
                            .address("15 Kim Mã, Ba Đình, Hà Nội")
                            .memberTier("Platinum")
                            .build(),
                    User.builder()
                            .fullName("Lê Hoàng Nam")
                            .email("nam.le@example.com")
                            .phoneNumber("0987654321")
                            .address("45 Nguyễn Văn Linh, Hải Châu, Đà Nẵng")
                            .memberTier("Standard")
                            .build()
            ));
        }
    }
}
