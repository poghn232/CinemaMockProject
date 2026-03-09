package com.example.superapp.repository;

import com.example.superapp.entity.MovieCredit;
import com.example.superapp.entity.MovieCreditId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCreditRepository extends JpaRepository<MovieCredit, MovieCreditId> {
}
