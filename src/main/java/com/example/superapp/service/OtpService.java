package com.example.superapp.service;

import com.example.superapp.dto.OtpData;
import com.example.superapp.entity.PendingUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class OtpService {

    private final Map<String, PendingUser> pendingUsers = new ConcurrentHashMap<>();
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final EmailService emailService;

    public OtpService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void createPendingUser(String email, String encodedPassword) {

        String otp = String.valueOf(
                new Random().nextInt(900000) + 100000
        );

        long expireTime =
                System.currentTimeMillis() + 5 * 60 * 1000;

        PendingUser pendingUser =
                new PendingUser(email, encodedPassword, expireTime);

        pendingUsers.put(email, pendingUser);
        otpStorage.put(email, otp);

        // 🔥 GỬI MAIL TẠI ĐÂY
        emailService.sendOtp(email, otp);
    }

    public boolean verifyOtp(String email, String otpInput) {

        PendingUser pendingUser = pendingUsers.get(email);
        String storedOtp = otpStorage.get(email);

        if (pendingUser == null || storedOtp == null)
            return false;

        if (System.currentTimeMillis() > pendingUser.getExpireTime()) {
            pendingUsers.remove(email);
            otpStorage.remove(email);
            return false;
        }

        return storedOtp.equals(otpInput);
    }

    public PendingUser getPendingUser(String email) {
        return pendingUsers.get(email);
    }

    public void removePendingUser(String email) {
        pendingUsers.remove(email);
        otpStorage.remove(email);
    }
}