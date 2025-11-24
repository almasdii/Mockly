package com.mockly.core.config;

import com.mockly.core.service.MinIOService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Initializes MinIO bucket on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinIOInitializer {

    private final MinIOService minIOService;

    @PostConstruct
    public void initialize() {
        log.info("Initializing MinIO bucket...");
        minIOService.initializeBucket();
        log.info("MinIO bucket initialization completed");
    }
}

