package com.example.superapp.service;

import com.example.superapp.config.VnPayProperties;
import com.example.superapp.entity.Payment;
import com.example.superapp.entity.PaymentStatus;
import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
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

    @Transactional
    public void handleCallback(Map<String, String> allParams) {
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
        String responseCode = allParams.get("vnp_ResponseCode");          // "00" = success
        String transactionStatus = allParams.get("vnp_TransactionStatus"); // thường "00" = success
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        Long paymentId = Long.valueOf(allParams.get("vnp_TxnRef"));
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 4) idempotent: nếu đã SUCCESS/FAILED rồi thì thôi
        if (payment.getStatus() != PaymentStatus.PENDING) return;

        Subscription sub = payment.getSubscription();

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());

            // set subscription ACTIVE + tính ngày
            sub.setStatus(SubscriptionStatus.ACTIVE);
            LocalDateTime start = LocalDateTime.now();
            sub.setStartDate(start);

            Integer durationDays = sub.getPack().getDurationDays();
            if (durationDays == null) durationDays = 30;
            sub.setEndDate(start.plusDays(durationDays));

            subRepo.save(sub);
            paymentRepo.save(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDate(LocalDateTime.now());

            sub.setStatus(SubscriptionStatus.FAILED);
            subRepo.save(sub);
            paymentRepo.save(payment);
        }
    }
}