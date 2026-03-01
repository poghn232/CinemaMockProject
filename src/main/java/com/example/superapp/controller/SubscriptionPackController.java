package com.example.superapp.controller;

import com.example.superapp.dto.SubscriptionPackDto;
import com.example.superapp.entity.SubscriptionPack;
import com.example.superapp.repository.SubscriptionPackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
public class SubscriptionPackController {

    private final SubscriptionPackRepository packRepo;

    @GetMapping
    public List<SubscriptionPackDto> all() {
        return packRepo.findAll()
                .stream()
                .map(p -> new SubscriptionPackDto(p.getPackId(), p.getPackName(), p.getPackPrice(), p.getDurationDays()))
                .toList();
    }
}
