package com.example.superapp.controller;

import com.example.superapp.dto.BuyPackRequest;
import com.example.superapp.dto.CreatePaymentRequest;
import com.example.superapp.service.PaymentService;
import com.example.superapp.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/vnpay")
    public Map<String, String> create(@RequestBody BuyPackRequest req,
                                      HttpServletRequest request,
                                      Authentication authentication) {

        String username = authentication.getName();
        String clientIp = IpUtil.getClientIp(request);

        String paymentUrl = paymentService.createVnPayUrlByUsername(username, req.getPackId(), clientIp);

        return Map.of("paymentUrl", paymentUrl);
    }
}