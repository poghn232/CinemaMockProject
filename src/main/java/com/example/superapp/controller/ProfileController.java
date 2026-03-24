package com.example.superapp.controller;

import com.example.superapp.dto.ProfileDto;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(("/api/profiles"))
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @GetMapping()
    public ResponseEntity<List<ProfileDto>> getAllProfiles(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
        List<ProfileDto> profiles = new ArrayList<>();
        currentUser.getProfiles().forEach(profile -> {
            profiles.add(new ProfileDto(profile.getProfileId(), profile.getProfileName(), profile.getUser().getUsername()));
        });
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable long id) {
        Profile profile = profileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cannot find profile with id: " + id));
        return ResponseEntity.ok(new ProfileDto(profile.getProfileId(), profile.getProfileName(), profile.getUser().getUsername()));
    }

    @PostMapping("/add")
    public ResponseEntity<ProfileDto> addProfile(@RequestBody ProfileDto dto) {

        System.out.println("dto profile name: " + dto.getUsername());

        User currentUser = userRepository.findByUsername(dto.getUsername())
                                         .orElseThrow(() -> new IllegalArgumentException("field profileName in ProfileController addProfile is empty/null or smth. cannot fetch user from db"));

        Profile newProfile = Profile.builder()
                                    .profileId(currentUser.getUserId() + 1)
                                    .profileName(dto.getProfileName())
                                    .user(currentUser)
                                    .build();
        Profile saved = profileRepository.save(newProfile);

        List<Profile> profiles = currentUser.getProfiles();
        profiles.add(newProfile);
        currentUser.setProfiles(profiles);
        userRepository.save(currentUser);
        return ResponseEntity.ok(new ProfileDto(saved.getProfileId(), saved.getProfileName(), saved.getUser().getUsername()));
    }

}
