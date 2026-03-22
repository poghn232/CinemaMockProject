package com.example.superapp.controller;

import com.example.superapp.dto.ChooseProfileDto;
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

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(("/api/profiles"))
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    @GetMapping()
    public ResponseEntity<List<ChooseProfileDto>> getAllProfiles(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        List<ChooseProfileDto> profiles = new ArrayList<>();
        currentUser.getProfiles().forEach(profile -> {
            profiles.add(new ChooseProfileDto(profile.getProfileId(), profile.getProfileName()));
        });
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChooseProfileDto> getProfile(@PathVariable long id) {
        Profile profile = profileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cannot find profile with id: " + id));
        return ResponseEntity.ok(new ChooseProfileDto(profile.getProfileId(), profile.getProfileName()));
    }

}
