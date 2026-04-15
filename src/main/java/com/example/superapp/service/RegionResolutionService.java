package com.example.superapp.service;

import com.example.superapp.utils.IpUtil;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegionResolutionService {

    private final JwtUtils jwtUtils;
    private final GeoIpService geoIpService;

    public String resolveRegion(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String region = jwtUtils.extractRegion(token);
                if (region != null && !region.isBlank()) {
                    String r = region.trim().toUpperCase();
                    // Login-time GeoIP may be UNKNOWN/LOCAL; prefer fresh GeoIP below when invalid.
                    if (!isUnusableRegionCode(r)) {
                        return r;
                    }
                }
            } catch (Exception ignored) {
                // fall through to GeoIP
            }
        }

        String ip = IpUtil.getClientIp(request);
        if (ip != null && !ip.isBlank()) {
            String geo = geoIpService.resolveRegion(ip);
            if (geo != null && !geo.isBlank() && !"UNKNOWN".equalsIgnoreCase(geo)) {
                String g = geo.trim().toUpperCase();
                if (!isUnusableRegionCode(g)) {
                    return g;
                }
            }
        }

        return null;
    }

    /** Placeholders from failed GeoIP / localhost — not comparable to ISO country blocks in DB. */
    private static boolean isUnusableRegionCode(String code) {
        if (code == null || code.isBlank()) {
            return true;
        }
        if ("UNKNOWN".equalsIgnoreCase(code)) {
            return true;
        }
        if ("LOCAL".equalsIgnoreCase(code)) {
            return true;
        }
        return "LOCAL-LOCAL".equalsIgnoreCase(code);
    }
}
