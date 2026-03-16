package com.example.superapp.repository;

import com.example.superapp.entity.Wishlist;
import com.example.superapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserAndContentIdAndContentType(User user, Long contentId, String contentType);
    boolean existsByUserAndContentIdAndContentType(User user, Long contentId, String contentType);
    List<Wishlist> findByUserOrderByAddedAtDesc(User user);
    void deleteByUserAndContentIdAndContentType(User user, Long contentId, String contentType);
    // Thêm vào WishlistRepository.java
    @Query("SELECT w.user FROM Wishlist w WHERE w.contentId = :contentId AND w.contentType = :contentType")
    List<User> findUsersByContentIdAndContentType(
            @Param("contentId") Long contentId,
            @Param("contentType") String contentType);
}