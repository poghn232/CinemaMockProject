package com.example.superapp.repository;

import com.example.superapp.entity.TvCredit;
import com.example.superapp.entity.TvCreditId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TvCreditRepository extends JpaRepository<TvCredit, TvCreditId> {
}
