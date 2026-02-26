package com.example.superapp.service;

import com.example.superapp.dto.RegisterRequest;
import com.example.superapp.dto.VerifyRequest;
import com.example.superapp.entity.PendingUser;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }


    // 🔹 Bước 1: Register
    public void register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        otpService.createPendingUser(
                request.getEmail(),
                encodedPassword
        );
    }

    // 🔹 Bước 2: Sau khi verify thành công
    public void createUserAfterVerify(String email) {

        PendingUser pendingUser =
                otpService.getPendingUser(email);

        if (pendingUser == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký");
        }

        User user = new User();
        user.setUsername(email);
        user.setPassword(pendingUser.getEncodedPassword());
        user.setRole("CUSTOMER");

        userRepository.save(user);

        otpService.removePendingUser(email);
    }
}
