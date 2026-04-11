package com.example.superapp.repository;

import com.example.superapp.entity.Profile;
import com.example.superapp.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    boolean existsByProfileAndContentIdAndContentType(Profile profile, Long contentId, String contentType);
    List<Wishlist> findByProfileOrderByAddedAtDesc(Profile profile);
    void deleteByProfileAndContentIdAndContentType(Profile profile, Long contentId, String contentType);

    @Query("SELECT w.profile FROM Wishlist w WHERE w.contentId = :contentId AND w.contentType = :contentType")
    List<Profile> findProfilesByContentIdAndContentType(
            @Param("contentId") Long contentId,
            @Param("contentType") String contentType);

    long countByProfile(Profile profile);
}