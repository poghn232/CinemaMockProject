package com.example.superapp.controller;

import com.example.superapp.service.VnPayCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Controller
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
            callbackService.handleCallback(params);

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");

            boolean success = "00".equals(responseCode)
                    && "00".equals(transactionStatus);

            if (success) {
                return "redirect:/homepage.html?payment=success";
            } else {
                return "redirect:/homepage.html?payment=fail";
            }

        } catch (Exception e) {
            return "redirect:/homepage.html?payment=fail";
        }
    }
}