package com.example.superapp.utils;

import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.CustomUserDetailsService;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không lấy được email từ Google");
            return;
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);

        // Nếu đã có tài khoản thì đăng nhập luôn
        if (existingUser != null) {
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(existingUser.getUsername());

            String token = jwtUtils.generateToken(userDetails);

            String redirectUrl = "http://localhost:8080/oauth2-success.html"
                    + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(existingUser.getUsername(), StandardCharsets.UTF_8)
                    + "&role=" + URLEncoder.encode(existingUser.getRole(), StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
            return;
        }

        // Nếu chưa có tài khoản thì chuyển sang trang tạo username
        String redirectUrl = "http://localhost:8080/choose-username.html"
                + "?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}