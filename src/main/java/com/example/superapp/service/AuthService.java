package com.example.superapp.service;

import com.example.superapp.dto.RegisterRequest;
import com.example.superapp.entity.PendingUser;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
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

    @Transactional
    public void forgotPassword(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username không được để trống");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Username không tồn tại"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Tài khoản chưa có email để nhận mật khẩu mới");
        }

        String newPassword = generateTempPassword(10);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendNewPassword(user.getEmail(), user.getUsername(), newPassword);
    }

    private String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        int safeLength = Math.max(8, length);

        StringBuilder sb = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            int idx = secureRandom.nextInt(alphabet.length());
            sb.append(alphabet.charAt(idx));
        }
        return sb.toString();
    }

    public void sendForgotOtp(String username) {

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Username không tồn tại"));

        otpService.createForgotPasswordOtp(username, user.getEmail());
    }
    public void verifyForgotOtp(String username, String otp) {

        boolean valid = otpService.verifyForgotOtp(username, otp);

        if (!valid) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }
    }
    public void resetPassword(String username, String newPassword) {

        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải >= 6 ký tự");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}