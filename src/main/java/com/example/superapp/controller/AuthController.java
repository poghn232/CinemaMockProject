package com.example.superapp.controller;

import com.example.superapp.dto.LoginRequest;
import com.example.superapp.dto.LoginResponse;
import com.example.superapp.dto.ForgotPasswordRequest;
import com.example.superapp.dto.RegisterRequest;
import com.example.superapp.dto.VerifyRequest;
import com.example.superapp.service.AuthService;
import com.example.superapp.service.CustomUserDetailsService;
import com.example.superapp.service.OtpService;
import com.example.superapp.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtils jwtUtil;
    private final AuthService authService;
    private final OtpService otpService;

    // ✅ CONSTRUCTOR INJECTION – RẤT QUAN TRỌNG
    public AuthController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtUtils jwtUtil,
            AuthService authService, OtpService otpService

    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getUsername());

        String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new LoginResponse(jwt));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok().body(
                Map.of("message", "OTP đã được gửi về email")
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request) {

        boolean isValid =
                otpService.verifyOtp(
                        request.getEmail(),
                        request.getOtp()
                );

        if (!isValid) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }

        authService.createUserAfterVerify(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "Đăng ký thành công")
        );
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {

        authService.sendForgotOtp(request.get("username"));

        return ResponseEntity.ok(
                Map.of("message", "OTP đã được gửi về email")
        );
    }
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {

        authService.verifyForgotOtp(
                request.get("username"),
                request.get("otp")
        );

        return ResponseEntity.ok(
                Map.of("message", "OTP hợp lệ")
        );
    }
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {

        authService.resetPassword(
                request.get("username"),
                request.get("newPassword")
        );

        return ResponseEntity.ok(
                Map.of("message", "Đổi mật khẩu thành công")
        );
    }
}