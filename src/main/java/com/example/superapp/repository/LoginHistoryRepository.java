package com.example.superapp.repository;

import com.example.superapp.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUsernameOrderByLoginTimeDesc(String username);

    @Query("""
        SELECT lh.countryCode, COUNT(lh)
        FROM LoginHistory lh
        WHERE lh.countryCode IS NOT NULL
        GROUP BY lh.countryCode
        ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> countLoginByCountry();

    @Query("""
        SELECT lh.region, COUNT(lh)
        FROM LoginHistory lh
        WHERE lh.region IS NOT NULL
        GROUP BY lh.region
        ORDER BY COUNT(lh) DESC
    """)
    List<Object[]> countLoginByRegion();
}