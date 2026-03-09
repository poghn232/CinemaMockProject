package com.example.superapp.service;

import com.example.superapp.entity.Banner;
import com.example.superapp.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

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
