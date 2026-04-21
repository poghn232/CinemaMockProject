package com.example.superapp.service;

import com.example.superapp.config.VnPayProperties;
import com.example.superapp.entity.*;
import com.example.superapp.repository.PaymentRepository;
import com.example.superapp.repository.SubscriptionPackRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.utils.VnPayUtil;
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
    private final UserRepository userRepo;

    @Transactional
    public String createVnPayUrlByUsername(String username, Long packId, Long subscriptionId, String clientIp) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPack pack = packRepo.findById(packId)
                .orElseThrow(() -> new RuntimeException("Pack not found"));

        Subscription sub;

        // CÁCH B: gia hạn subscription cũ
        if (subscriptionId != null) {
            sub = subRepo.findById(subscriptionId)
                    .orElseThrow(() -> new RuntimeException("Subscription not found"));

            // kiểm tra subscription này có thuộc user hiện tại không
            if (!sub.getUser().getUserId().equals(user.getUserId())) {
                throw new RuntimeException("You cannot extend another user's subscription");
            }

            // chỉ cho gia hạn subscription đang active
            if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new RuntimeException("Only active subscription can be extended");
            }

            // cập nhật pack mới mà user vừa chọn để lúc callback dùng durationDays mới
            sub.setPack(pack);
            subRepo.save(sub);

        } else {
            // MUA MỚI
            sub = new Subscription();
            sub.setUser(user);
            sub.setPack(pack);
            sub.setStatus(SubscriptionStatus.PENDING);
            sub = subRepo.save(sub);
        }

        // Tạo payment PENDING
        Payment payment = new Payment();
        payment.setSubscription(sub);
        payment.setAmount(new BigDecimal(pack.getPackPrice().replace(".", "")));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepo.save(payment);

        // Build VNPay params
        String txnRef = String.valueOf(payment.getPaymentId());
        
        // Parse string price (remove dots) to number
        String cleanPrice = pack.getPackPrice().replace(".", "");
        BigDecimal numericPrice = new BigDecimal(cleanPrice);
        long amountVnp = numericPrice.multiply(BigDecimal.valueOf(100)).longValue();

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