package com.example.superapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

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
}