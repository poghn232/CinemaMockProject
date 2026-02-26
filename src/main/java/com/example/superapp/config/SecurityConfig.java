package com.example.superapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ FRONTEND
                        .requestMatchers(
                                "/login.html",
                                "/register.html",
                                "/home.html",
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ✅ AUTH API
                        .requestMatchers("/api/auth/**").permitAll()

                        // ❌ còn lại phải login
                        .anyRequest().authenticated()
                )

                // ❌ không dùng form login mặc định
                .formLogin(form -> form.disable())

                // ❌ không dùng basic auth
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // 🔥 BẮT BUỘC – NẾU THIẾU → LỖI CỦA BẠN
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 🔐 BẮT BUỘC nếu dùng password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}