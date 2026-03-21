package com.example.superapp.repository;

import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByProfileName(String profileName);
}
