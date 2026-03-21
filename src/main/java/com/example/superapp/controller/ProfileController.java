package com.example.superapp.controller;

import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(("/api/profiles"))
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @GetMapping()
    public ResponseEntity<List<Profile>> getAllProfiles(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        return ResponseEntity.ok(currentUser.getProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfile(@PathVariable long id) {
        return ResponseEntity.ok(profileRepository.findById(id).
                                                  orElseThrow(
                                                      () -> new IllegalArgumentException("Id is invalid")
                                                  )
        );
    }
}
