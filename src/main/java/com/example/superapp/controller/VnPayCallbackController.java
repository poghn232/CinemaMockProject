package com.example.superapp.controller;

import com.example.superapp.service.VnPayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayCallbackService callbackService;

    // IPN: VNPay gọi server-to-server
    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        try {
            callbackService.handleCallback(params);
            // theo spec thường trả:
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (Exception e) {
            return Map.of("RspCode", "99", "Message", "Confirm Fail");
        }
    }

    // Return: user redirect về (chỉ hiển thị)
    @GetMapping("/return")
    public String vnpReturn(@RequestParam Map<String, String> params) {
        try {
            callbackService.handleCallback(params); // có thể gọi, nhưng IPN mới là chuẩn
            return "Thanh toán xử lý xong. Bạn có thể quay lại app.";
        } catch (Exception e) {
            return "Thanh toán thất bại hoặc chữ ký không hợp lệ.";
        }
    }
}