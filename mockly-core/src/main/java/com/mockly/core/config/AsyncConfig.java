package com.mockly.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async processing.
 * Enables @Async support and configures thread pool for report processing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reportProcessingExecutor")
    public Executor reportProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-processing-");
        executor.initialize();
        return executor;
    }
}

