package com.example.superapp.config;

import com.example.superapp.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ JWT stateless
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
<<<<<<< HEAD
                                "/login.html", "/register.html", "/home.html", "/packs.html",
                                "/", "/index.html",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/api/auth/**",

                                // ✅ VNPay callbacks
                                "/api/vnpay/**",
                                "/api/payment/**",      // nếu dùng path này
                                "/api/payments/**"
                        ).permitAll()
=======
                                "/login.html",
                                "/register.html",
                                "/forgot-password.html",
                                "/homepage.html",
                                "/home.html",
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ✅ PUBLIC API
                        .requestMatchers("/api/auth/**").permitAll()

                        // ❌ còn lại phải login
>>>>>>> 56d71de (add forget and homepage(not done))
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}