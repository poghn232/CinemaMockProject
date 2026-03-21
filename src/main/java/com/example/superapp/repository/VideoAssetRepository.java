package com.example.superapp.repository;

import com.example.superapp.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
    Optional<VideoAsset> findTopByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(String ownerType, Long ownerId);
}