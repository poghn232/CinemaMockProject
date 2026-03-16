package com.example.superapp.service;

public interface GeoIpService {
    String resolveRegion(String ip);
    String resolveCountry(String ip);
    boolean isLocalIp(String ip);
}