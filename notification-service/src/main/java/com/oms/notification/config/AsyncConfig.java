package com.oms.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing
 * Enables @Async annotation support for non-blocking operations
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Value("${async.core-pool-size:5}")
    private int corePoolSize;
    
    @Value("${async.max-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${async.queue-capacity:100}")
    private int queueCapacity;
    
    /**
     * Task executor for email sending operations
     * Ensures email sending doesn't block the main thread
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}