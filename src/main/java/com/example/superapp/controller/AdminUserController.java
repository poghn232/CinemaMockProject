package com.example.superapp.controller;

import com.example.superapp.dto.UserAdminDto;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserAdminDto> all() {
    return userRepository.findAll()
        .stream()
        // only return non-admin users to the admin UI
        .filter(u -> u.getRole() == null || !u.getRole().toUpperCase().contains("ADMIN"))
        .map(u -> new UserAdminDto(u.getUserId(), u.getUsername(), u.getEmail(), u.getRole(), u.getEnabled()))
        .toList();
    }

    @PutMapping("/{id}/enabled")
    public UserAdminDto setEnabled(@PathVariable Long id, @RequestBody Boolean enabled) {
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    // disallow changing admin accounts
    if (u.getRole() != null && u.getRole().toUpperCase().contains("ADMIN")) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify admin account");
    }
    u.setEnabled(enabled);
        User saved = userRepository.save(u);
        return new UserAdminDto(saved.getUserId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getEnabled());
    }
}
