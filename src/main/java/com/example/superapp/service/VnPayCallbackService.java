package com.example.superapp.service;

import com.example.superapp.config.VnPayProperties;
import com.example.superapp.entity.*;
import com.example.superapp.repository.PaymentRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.utils.VnPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnPayCallbackService {

    private final VnPayProperties vnp;
    private final PaymentRepository paymentRepo;
    private final SubscriptionRepository subRepo;
    private final EmailService emailService;

    @Transactional
    public Subscription handleCallback(Map<String, String> allParams) {
        // 1) tách secureHash
        String receivedHash = allParams.get("vnp_SecureHash");
        Map<String, String> params = new HashMap<>(allParams);
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        // 2) verify chữ ký
        String hashData = VnPayUtil.buildHashData(params);
        String expectedHash = VnPayUtil.hmacSHA512(vnp.getHashSecret(), hashData);
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            throw new RuntimeException("Invalid VNPay signature");
        }

        // 3) kiểm tra kết quả
        String responseCode = allParams.get("vnp_ResponseCode");
        String transactionStatus = allParams.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        Long paymentId = Long.valueOf(allParams.get("vnp_TxnRef"));
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 4) idempotent: nếu đã xử lý rồi thì trả luôn subscription hiện tại
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getSubscription();
        }

        Subscription sub = payment.getSubscription();

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());

            Integer durationDays = sub.getPack().getDurationDays();
            if (durationDays == null) {
                durationDays = 30;
            }

            LocalDateTime now = LocalDateTime.now();

            // CASE 1: MUA MỚI
            if (sub.getStatus() == SubscriptionStatus.PENDING) {
                LocalDateTime start = now;
                LocalDateTime end = start.plusDays(durationDays);

                sub.setStartDate(start);
                sub.setEndDate(end);
                sub.setStatus(SubscriptionStatus.ACTIVE);
            }

            // CASE 2: GIA HẠN GÓI CŨ
            else if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                LocalDateTime baseEnd = sub.getEndDate();

                if (baseEnd == null || baseEnd.isBefore(now)) {
                    baseEnd = now;
                }

                sub.setEndDate(baseEnd.plusDays(durationDays));
                sub.setStatus(SubscriptionStatus.ACTIVE);
            }

            // fallback
            else {
                LocalDateTime baseEnd = sub.getEndDate();

                if (baseEnd == null || baseEnd.isBefore(now)) {
                    baseEnd = now;
                }

                if (sub.getStartDate() == null) {
                    sub.setStartDate(now);
                }

                sub.setEndDate(baseEnd.plusDays(durationDays));
                sub.setStatus(SubscriptionStatus.ACTIVE);
            }

            User user = sub.getUser();
            SubscriptionPack pack = sub.getPack();

            subRepo.save(sub);
            paymentRepo.save(payment);

            try {
                emailService.sendSubscriptionSuccessEmail(
                        user.getEmail(),
                        user.getUsername(),
                        pack.getPackName(),
                        pack.getPackPrice(),
                        sub.getStartDate(),
                        sub.getEndDate()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            return sub;

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDate(LocalDateTime.now());

            // Chỉ set FAILED cho subscription mới tạo ra khi mua mới
            if (sub.getStatus() == SubscriptionStatus.PENDING) {
                sub.setStatus(SubscriptionStatus.FAILED);
                subRepo.save(sub);
            }

            paymentRepo.save(payment);
            throw new RuntimeException("Payment failed");
        }
    }
}