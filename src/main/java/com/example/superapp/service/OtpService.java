package com.example.superapp.service;

import com.example.superapp.entity.PendingUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // lưu user đang chờ verify
    private final Map<String, PendingUser> pendingUsers = new ConcurrentHashMap<>();

    // lưu otp theo email
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    private final EmailService emailService;

    private final Map<String, String> forgotOtpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> forgotOtpExpire = new ConcurrentHashMap<>();

    public OtpService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Tạo PendingUser + OTP
     */
    public void createPendingUser(String username, String email, String encodedPassword) {

        // tạo OTP 6 số
        String otp = String.valueOf(
                new Random().nextInt(900000) + 100000
        );

        // hết hạn sau 5 phút
        long expireTime =
                System.currentTimeMillis() + 5 * 60 * 1000;

        // tạo PendingUser
        PendingUser pendingUser =
                new PendingUser(
                        email,encodedPassword,expireTime,username
                );

        // lưu vào memory
        pendingUsers.put(email, pendingUser);
        otpStorage.put(email, otp);

        // gửi email
        emailService.sendOtp(email, otp);
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String email, String otpInput) {

        PendingUser pendingUser = pendingUsers.get(email);
        String storedOtp = otpStorage.get(email);

        if (pendingUser == null || storedOtp == null)
            return false;

        // check expire
        if (System.currentTimeMillis() > pendingUser.getExpireTime()) {

            pendingUsers.remove(email);
            otpStorage.remove(email);

            return false;
        }

        // check otp đúng
        return storedOtp.equals(otpInput);
    }

    /**
     * lấy pending user
     */
    public PendingUser getPendingUser(String email) {
        return pendingUsers.get(email);
    }

    /**
     * xoá pending user sau khi verify thành công
     */
    public void removePendingUser(String email) {

        pendingUsers.remove(email);
        otpStorage.remove(email);
    }

    public void createForgotPasswordOtp(String username, String email) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;

        forgotOtpStorage.put(username, otp);
        forgotOtpExpire.put(username, expireTime);

        emailService.sendOtp(email, otp);
    }
    public boolean verifyForgotOtp(String username, String otpInput) {

        String storedOtp = forgotOtpStorage.get(username);
        Long expireTime = forgotOtpExpire.get(username);

        if (storedOtp == null || expireTime == null)
            return false;

        if (System.currentTimeMillis() > expireTime) {
            forgotOtpStorage.remove(username);
            forgotOtpExpire.remove(username);
            return false;
        }

        return storedOtp.equals(otpInput);
    }
}