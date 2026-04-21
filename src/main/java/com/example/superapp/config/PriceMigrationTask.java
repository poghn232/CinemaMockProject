package com.example.superapp.config;

import com.example.superapp.entity.SubscriptionPack;
import com.example.superapp.repository.SubscriptionPackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class PriceMigrationTask {

    private static final Logger log = LoggerFactory.getLogger(PriceMigrationTask.class);
    private final SubscriptionPackRepository packRepo;

    @Bean
    public CommandLineRunner migratePrices() {
        return args -> {
            log.info("Starting Price Migration Task (Cleaning up existing data)...");
            List<SubscriptionPack> packs = packRepo.findAll();
            boolean changed = false;

            for (SubscriptionPack pack : packs) {
                String original = pack.getPackPrice();
                if (original == null || original.isEmpty()) continue;

                // 1. Remove .00 suffix if exists
                String cleaned = original.split("\\.")[0];
                
                // 2. Remove any other characters that are not digits
                cleaned = cleaned.replaceAll("\\D", "");
                
                if (cleaned.isEmpty()) continue;

                try {
                    long priceVal = Long.parseLong(cleaned);
                    // Format with dots as thousand separators manually to avoid locale issues
                    String formatted = formatWithDots(priceVal);
                    
                    if (!formatted.equals(original)) {
                        pack.setPackPrice(formatted);
                        packRepo.save(pack);
                        changed = true;
                        log.info("Updated pack '{}' price: {} -> {}", pack.getPackName(), original, formatted);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Could not parse price for pack {}: {}", pack.getPackName(), cleaned);
                }
            }

            if (!changed) {
                log.info("No prices needed migration.");
            } else {
                log.info("Price migration completed.");
            }
        };
    }

    private String formatWithDots(long val) {
        return String.format("%,d", val).replace(',', '.');
    }
}
