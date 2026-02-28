package com.example.superapp.service;

import com.example.superapp.dto.RegisterRequest;
import com.example.superapp.entity.PendingUser;
import com.example.superapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    public void register(RegisterRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("Username không được để trống");
        }

        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu phải >= 6 ký tự");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username đã tồn tại");
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        otpService.createPendingUser(
                request.getUsername(),
                request.getEmail(),
                encodedPassword
        );
    }

    public void createUserAfterVerify(String email) {

        PendingUser pendingUser =
                otpService.getPendingUser(email);

        if (pendingUser == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký");
        }

        User user = new User();

        user.setUsername(pendingUser.getUsername());
        user.setEmail(pendingUser.getEmail());
        user.setPassword(pendingUser.getEncodedPassword());
        user.setRole("CUSTOMER");

        userRepository.save(user);

        otpService.removePendingUser(email);
    }
}