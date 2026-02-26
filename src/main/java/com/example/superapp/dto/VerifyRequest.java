package com.example.superapp.dto;

public class VerifyRequest {
    private String otp;
    private String email;


    public VerifyRequest(String otp, String email) {
        this.otp = otp;
        this.email = email;
    }

    public VerifyRequest() {
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
