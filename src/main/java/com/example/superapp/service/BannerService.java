package com.example.superapp.service;

import com.example.superapp.entity.Banner;
import com.example.superapp.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    public Banner getBannerById(int bannerId) {
        return bannerRepository.findById(bannerId).orElse(null);
    }

    public void save(Banner banner) {
        bannerRepository.save(banner);
    }
}
