package com.example.superapp.service;

import com.example.superapp.entity.Ad;
import com.example.superapp.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdService {

    private final AdRepository adRepository;

    public Ad getActivePreRollAd() {
        return adRepository.findFirstByAdTypeAndStatusOrderByIdDesc("PRE_ROLL", "ACTIVE")
                .orElse(null);
    }
}