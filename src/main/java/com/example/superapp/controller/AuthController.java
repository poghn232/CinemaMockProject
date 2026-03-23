package com.example.superapp.controller;

import com.example.superapp.dto.*;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.*;
import com.example.superapp.utils.GoogleTokenVerifier;
import com.example.superapp.utils.FacebookTokenVerifier;
import com.example.superapp.utils.FacebookTokenVerifier.FacebookUserInfo;
import com.example.superapp.utils.IpUtil;
import com.example.superapp.utils.JwtUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtil;
    private final AuthService authService;
    private final OtpService otpService;
    private final GeoIpService geoIpService;
    private final LoginHistoryService loginHistoryService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final FacebookTokenVerifier facebookTokenVerifier;

    public AuthController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository,
            JwtUtils jwtUtil,
            AuthService authService,
            OtpService otpService,
            GeoIpService geoIpService,
            LoginHistoryService loginHistoryService,
            GoogleTokenVerifier googleTokenVerifier,
            FacebookTokenVerifier facebookTokenVerifier
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.otpService = otpService;
        this.geoIpService = geoIpService;
        this.loginHistoryService = loginHistoryService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.facebookTokenVerifier = facebookTokenVerifier;
    }

    // ─── Normal login ────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_CUSTOMER");

        String clientIp = IpUtil.getClientIp(httpRequest);
        String region = geoIpService.resolveRegion(clientIp);
        String jwt = jwtUtil.generateToken(userDetails, region, clientIp);

        LoginResponse resp = new LoginResponse(jwt);
        resp.setRole(role);
        resp.setRegion(region);

        boolean requirePublicIp = geoIpService.isLocalIp(clientIp)
                || "LOCAL-LOCAL".equalsIgnoreCase(region)
                || "LOCAL".equalsIgnoreCase(region);
        resp.setRequirePublicIp(requirePublicIp);

        if (!requirePublicIp && role.toUpperCase().contains("CUSTOMER")) {
            loginHistoryService.saveLoginHistory(request.getUsername(), clientIp, region);
        }

        return ResponseEntity.ok(resp);
    }

    // ─── Google OAuth2 login ─────────────────────────────────────────────────
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest
    ) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "idToken không được để trống"));
        }

        GoogleIdToken.Payload payload;
        try {
            payload = googleTokenVerifier.verify(idToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Google token không hợp lệ"));
        }

        String email = payload.getEmail();
        String googleId = payload.getSubject();

        // Find existing user by googleId or email, or create a new one
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .username(email)
                    .googleId(googleId)
                    .password(null)
                    .role("CUSTOMER")
                    .enabled(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Sync googleId in case the account was created via normal register first
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String clientIp = IpUtil.getClientIp(httpRequest);
        String region = geoIpService.resolveRegion(clientIp);
        String jwt = jwtUtil.generateToken(userDetails, region, clientIp);

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_CUSTOMER");

        LoginResponse resp = new LoginResponse(jwt);
        resp.setRole(role);
        resp.setRegion(region);

        boolean requirePublicIp = geoIpService.isLocalIp(clientIp)
                || "LOCAL-LOCAL".equalsIgnoreCase(region)
                || "LOCAL".equalsIgnoreCase(region);
        resp.setRequirePublicIp(requirePublicIp);

        if (!requirePublicIp) {
            loginHistoryService.saveLoginHistory(user.getUsername(), clientIp, region);
        }

        return ResponseEntity.ok(resp);
    }

    // ─── Facebook OAuth2 login ──────────────────────────────────────────────────
    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        String accessToken = body.get("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "accessToken is required"));
        }

        FacebookUserInfo fbUser;
        try {
            fbUser = facebookTokenVerifier.verify(accessToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid Facebook token"));
        }

        String email = fbUser.getEmail();
        String fbId = fbUser.getId();

        // Lookup order: facebookId → email (if present) → create new
        User user = userRepository.findByFacebookId(fbId).orElse(null);

        if (user == null && email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            String username = (email != null && !email.isBlank()) ? email : "fb_" + fbId;
            if (userRepository.existsByUsername(username)) {
                username = "fb_" + fbId;
            }
            user = User.builder()
                    .email(email != null && !email.isBlank() ? email : "fb_" + fbId + "@facebook.com")
                    .username(username)
                    .facebookId(fbId)
                    .password(null)
                    .role("CUSTOMER")
                    .enabled(true)
                    .build();
            user = userRepository.save(user);
        }

        // Sync facebookId if missing
        if (user.getFacebookId() == null) {
            user.setFacebookId(fbId);
            userRepository.save(user);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String clientIp = IpUtil.getClientIp(httpRequest);
        String region = geoIpService.resolveRegion(clientIp);
        String jwt = jwtUtil.generateToken(userDetails, region, clientIp);

        String role = userDetails.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("ROLE_CUSTOMER");

        LoginResponse resp = new LoginResponse(jwt);
        resp.setRole(role);
        resp.setRegion(region);
        boolean requirePublicIp = geoIpService.isLocalIp(clientIp)
                || "LOCAL-LOCAL".equalsIgnoreCase(region)
                || "LOCAL".equalsIgnoreCase(region);
        resp.setRequirePublicIp(requirePublicIp);

        if (!requirePublicIp) {
            loginHistoryService.saveLoginHistory(user.getUsername(), clientIp, region);
        }

        return ResponseEntity.ok(resp);
    }

    // ─── Register ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "OTP đã được gửi về email"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }
        authService.createUserAfterVerify(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }

    // ─── Forgot password ──────────────────────────────────────────────────────
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        authService.sendForgotOtp(request.get("username"));
        return ResponseEntity.ok(Map.of("message", "OTP đã được gửi về email"));
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        authService.verifyForgotOtp(request.get("username"), request.get("otp"));
        return ResponseEntity.ok(Map.of("message", "OTP hợp lệ"));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        authService.resetPassword(request.get("username"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    // ─── Resolve region from public IP ───────────────────────────────────────
    @PostMapping("/resolve-region")
    public ResponseEntity<?> resolveRegionFromPublicIp(
            @RequestBody ResolveRegionRequest request,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Thiếu token"));
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String publicIp = request.getPublicIp();
        if (publicIp == null || publicIp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Public IP không được để trống"));
        }

        String region = geoIpService.resolveRegion(publicIp);
        String newToken = jwtUtil.generateToken(userDetails, region, publicIp);

        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_CUSTOMER");

        LoginResponse resp = new LoginResponse(newToken);
        resp.setRole(role);
        resp.setRegion(region);
        resp.setRequirePublicIp(false);

        if (role.toUpperCase().contains("CUSTOMER")) {
            loginHistoryService.saveLoginHistory(username, publicIp, region);
        }

        return ResponseEntity.ok(resp);
    }
}