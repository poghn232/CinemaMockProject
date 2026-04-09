package com.example.superapp;

import com.example.superapp.service.R2StorageService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockProjectApplication {


    public static void main(String[] args) {
        SpringApplication.run(MockProjectApplication.class, args);
    }
}
