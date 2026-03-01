package com.example.superapp.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class VnPayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) hash.append(String.format("%02x", b));
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot sign VNPay", e);
        }
    }

    // VNPay thường encode theo kiểu querystring (space -> '+')
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
        // nếu vẫn lỗi, thử StandardCharsets.US_ASCII (một số sample VNPay dùng US_ASCII)
    }

    // Query string: sort key, encode value
    public static String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    // Hash data: sort key, encode value Y HỆT query, và LOẠI secureHash fields
    public static String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .filter(e -> !"vnp_SecureHash".equals(e.getKey()) && !"vnp_SecureHashType".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }
}