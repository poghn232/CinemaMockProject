package com.example.superapp.service;

import com.example.superapp.dto.GeoIpResponse;
import com.example.superapp.dto.IpWhoIsResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeoIpServiceImpl implements GeoIpService {

    private final RestTemplate restTemplate;

    public GeoIpServiceImpl(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    private boolean isLoopbackIp(String ip) {
        return "127.0.0.1".equals(ip)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private GeoIpResponse buildLocalResponse() {
        GeoIpResponse local = new GeoIpResponse();
        local.setCountryCode("LOCAL");
        local.setRegion("LOCAL");
        local.setRegionCode("LOCAL");
        return local;
    }

    private GeoIpResponse lookupFromIpApiCo(String ip) {
        try {
            if (ip == null || ip.isBlank()) {
                System.out.println("lookupFromIpApiCo: ip null/blank");
                return null;
            }

            if (isLoopbackIp(ip)) {
                return buildLocalResponse();
            }

            String url = "https://ipapi.co/" + ip + "/json/";
            System.out.println("Calling ipapi.co: " + url);

            GeoIpResponse response = restTemplate.getForObject(url, GeoIpResponse.class);

            System.out.println("ipapi.co countryCode = " + (response != null ? response.getCountryCode() : null));
            System.out.println("ipapi.co regionCode = " + (response != null ? response.getRegionCode() : null));
            System.out.println("ipapi.co region = " + (response != null ? response.getRegion() : null));

            return response;

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.out.println("ipapi.co bị rate limit (429) cho IP = " + ip);
            return null;
        } catch (Exception e) {
            System.out.println("ipapi.co lookup failed for IP = " + ip);
            e.printStackTrace();
            return null;
        }
    }

    private GeoIpResponse lookupFromIpWhoIs(String ip) {
        try {
            if (ip == null || ip.isBlank()) {
                System.out.println("lookupFromIpWhoIs: ip null/blank");
                return null;
            }

            if (isLoopbackIp(ip)) {
                return buildLocalResponse();
            }

            String url = "https://ipwho.is/" + ip;
            System.out.println("Calling ipwho.is: " + url);

            IpWhoIsResponse response = restTemplate.getForObject(url, IpWhoIsResponse.class);

            if (response == null) {
                System.out.println("ipwho.is response null");
                return null;
            }

            System.out.println("ipwho.is success = " + response.isSuccess());
            System.out.println("ipwho.is countryCode = " + response.getCountryCode());
            System.out.println("ipwho.is regionCode = " + response.getRegionCode());
            System.out.println("ipwho.is region = " + response.getRegion());
            System.out.println("ipwho.is message = " + response.getMessage());

            if (!response.isSuccess()) {
                return null;
            }

            GeoIpResponse mapped = new GeoIpResponse();
            mapped.setIp(response.getIp());
            mapped.setCountryCode(response.getCountryCode());
            mapped.setCountryName(response.getCountryName());
            mapped.setRegion(response.getRegion());
            mapped.setRegionCode(response.getRegionCode());

            return mapped;

        } catch (Exception e) {
            System.out.println("ipwho.is lookup failed for IP = " + ip);
            e.printStackTrace();
            return null;
        }
    }

    private GeoIpResponse lookup(String ip) {
        GeoIpResponse primary = lookupFromIpApiCo(ip);

        if (primary != null && primary.getCountryCode() != null && !primary.getCountryCode().isBlank()) {
            System.out.println("Using ipapi.co result");
            return primary;
        }

        System.out.println("Falling back to ipwho.is for IP = " + ip);
        GeoIpResponse fallback = lookupFromIpWhoIs(ip);

        if (fallback != null && fallback.getCountryCode() != null && !fallback.getCountryCode().isBlank()) {
            System.out.println("Using ipwho.is result");
            return fallback;
        }

        System.out.println("Both geo providers failed for IP = " + ip);
        return null;
    }

    @Override
    public String resolveRegion(String ip) {
        GeoIpResponse res = lookup(ip);

        System.out.println("resolveRegion ip = " + ip);
        System.out.println("resolveRegion countryCode = " + (res != null ? res.getCountryCode() : null));
        System.out.println("resolveRegion regionCode = " + (res != null ? res.getRegionCode() : null));

        if (res == null) return "UNKNOWN";

        if (res.getCountryCode() == null || res.getCountryCode().isBlank()) {
            return "UNKNOWN";
        }

        return res.getCountryCode().trim().toUpperCase();
    }

    @Override
    public String resolveCountry(String ip) {
        GeoIpResponse res = lookup(ip);
        if (res == null || res.getCountryCode() == null || res.getCountryCode().isBlank()) {
            return "UNKNOWN";
        }
        return res.getCountryCode();
    }

    @Override
    public boolean isLocalIp(String ip) {
        return ip != null && (
                ip.equals("127.0.0.1")
                        || ip.equals("::1")
                        || ip.equals("0:0:0:0:0:0:0:1")
                        || ip.startsWith("192.168.")
                        || ip.startsWith("10.")
                        || ip.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*")
        );
    }
}