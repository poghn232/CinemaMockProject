package com.example.superapp.service;

import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String u = username.trim();
        User user = userRepository.findByUsername(u)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        System.out.println("LOGIN username = [" + u + "]");
        System.out.println("DB password = " + user.getPassword());
        System.out.println("DB pass len = " + (user.getPassword() == null ? 0 : user.getPassword().length()));

        String rawRole = user.getRole();
        String roleForSpring = "CUSTOMER";
        if (rawRole != null && !rawRole.isBlank()) {
            rawRole = rawRole.trim();
            roleForSpring = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        }

        // Google-login users have no password — use a blank placeholder.
        // Spring Security won't check it because we issue our JWT directly
        // in AuthController without going through AuthenticationManager.
        String password = user.getPassword() != null ? user.getPassword() : "";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(password)
                .roles(roleForSpring)
                .disabled(user.getEnabled() == null ? false : !user.getEnabled())
                .build();
    }
}