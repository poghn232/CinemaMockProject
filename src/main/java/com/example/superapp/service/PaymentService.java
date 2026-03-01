package com.example.superapp.service;

import com.example.superapp.config.VnPayProperties;
import com.example.superapp.utils.VnPayUtil;
import com.example.superapp.entity.*;
import com.example.superapp.repository.PaymentRepository;
import com.example.superapp.repository.SubscriptionPackRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VnPayProperties vnp;
    private final SubscriptionPackRepository packRepo;
    private final SubscriptionRepository subRepo;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo; // bạn đã có entity User

    @Transactional
    public String createVnPayUrlByUsername(String username, Long packId, String clientIp) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPack pack = packRepo.findById(packId)
                .orElseThrow(() -> new RuntimeException("Pack not found"));

        // Subscription PENDING
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setPack(pack);
        sub.setStatus(SubscriptionStatus.PENDING);
        sub = subRepo.save(sub);

        // Payment PENDING
        Payment payment = new Payment();
        payment.setSubscription(sub);
        payment.setAmount(pack.getPackPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepo.save(payment);

        // Build VNPay params
        String txnRef = String.valueOf(payment.getPaymentId());
        long amountVnp = pack.getPackPrice().multiply(BigDecimal.valueOf(100)).longValue();

        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnp.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountVnp));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Pay pack " + pack.getPackName() + " subId=" + sub.getSubId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnp.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        String hashData = VnPayUtil.buildHashData(params);
        String secureHash = VnPayUtil.hmacSHA512(vnp.getHashSecret(), hashData);
        params.put("vnp_SecureHash", secureHash);

        return vnp.getPayUrl() + "?" + VnPayUtil.buildQueryString(params);
    }
}