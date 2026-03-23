package com.example.superapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Kích hoạt @Async để NotificationService.notifyWishlistUsers() chạy
 * trên thread riêng, không block admin request.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot sẽ dùng SimpleAsyncTaskExecutor mặc định.
    // Nếu muốn tuỳ chỉnh thread pool, uncomment bên dưới:

    // @Bean
    // public Executor notificationExecutor() {
    //     ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    //     ex.setCorePoolSize(2);
    //     ex.setMaxPoolSize(5);
    //     ex.setQueueCapacity(100);
    //     ex.setThreadNamePrefix("notif-");
    //     ex.initialize();
    //     return ex;
    // }
    @Bean(name = "videoTaskExecutor")
    public Executor videoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("video-encode-");
        executor.initialize();
        return executor;
    }
}