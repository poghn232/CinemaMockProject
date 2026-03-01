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

        String u = username.trim(); // rất nên trim
        User user = userRepository.findByUsername(u)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        System.out.println("LOGIN username = [" + u + "]");
        System.out.println("DB password = " + user.getPassword());
        System.out.println("DB pass len = " + (user.getPassword() == null ? 0 : user.getPassword().length()));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}
