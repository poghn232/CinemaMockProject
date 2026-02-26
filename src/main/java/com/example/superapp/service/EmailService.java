package com.example.superapp.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã OTP xác thực đăng ký");
        message.setText(
                "Xin chào,\n\n" +
                        "Mã OTP của bạn là: " + otp +
                        "\nOTP có hiệu lực trong 5 phút.\n\n" +
                        "Không chia sẻ mã này cho người khác."
        );

        mailSender.send(message);
    }
}
