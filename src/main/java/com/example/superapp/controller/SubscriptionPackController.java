package com.example.superapp.controller;

import com.example.superapp.dto.SubscriptionPackDto;
import com.example.superapp.entity.SubscriptionPack;
import com.example.superapp.repository.SubscriptionPackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping
    public SubscriptionPackDto create(@RequestBody SubscriptionPackDto dto) {
        SubscriptionPack p = new SubscriptionPack();
        p.setPackName(dto.packName());
        p.setPackPrice(dto.packPrice());
        p.setDurationDays(dto.durationDays());
        SubscriptionPack saved = packRepo.save(p);
        return new SubscriptionPackDto(saved.getPackId(), saved.getPackName(), saved.getPackPrice(), saved.getDurationDays());
    }

    @PutMapping("/{id}")
    public SubscriptionPackDto update(@PathVariable Long id, @RequestBody SubscriptionPackDto dto) {
        SubscriptionPack p = packRepo.findById(id).orElseThrow(() -> new RuntimeException("Pack not found"));
        p.setPackName(dto.packName());
        p.setPackPrice(dto.packPrice());
        p.setDurationDays(dto.durationDays());
        SubscriptionPack saved = packRepo.save(p);
        return new SubscriptionPackDto(saved.getPackId(), saved.getPackName(), saved.getPackPrice(), saved.getDurationDays());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        packRepo.deleteById(id);
    }
}
