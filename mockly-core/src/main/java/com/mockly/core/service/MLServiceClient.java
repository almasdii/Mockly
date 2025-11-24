package com.mockly.core.service;

import com.mockly.core.dto.ml.MLProcessRequest;
import com.mockly.core.dto.ml.MLProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for communicating with ML service.
 * Handles async requests to process artifacts and generate reports.
 */
@Service
@Slf4j
public class MLServiceClient {

    private final WebClient mlServiceWebClient;

    public MLServiceClient(@Qualifier("mlServiceWebClient") WebClient mlServiceWebClient) {
        this.mlServiceWebClient = mlServiceWebClient;
    }

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(30); // ML processing can take time

    /**
     * Send artifact to ML service for processing.
     *
     * @param request Processing request with artifact details
     * @return ML processing response with metrics, summary, recommendations, and transcript
     */
    public MLProcessResponse processArtifact(MLProcessRequest request) {
        log.info("Sending artifact to ML service for processing: sessionId={}, artifactId={}", 
                request.sessionId(), request.artifactId());

        try {
            MLProcessResponse response = mlServiceWebClient.post()
                    .uri("/api/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MLProcessResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            log.info("ML service processing completed for session: {}", request.sessionId());
            return response;
        } catch (WebClientResponseException e) {
            log.error("ML service error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("ML service processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling ML service", e);
            throw new RuntimeException("Failed to process artifact with ML service", e);
        }
    }

    /**
     * Check if ML service is available.
     *
     * @return true if service is available
     */
    public boolean isAvailable() {
        try {
            mlServiceWebClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("ML service health check failed", e);
            return false;
        }
    }
}

