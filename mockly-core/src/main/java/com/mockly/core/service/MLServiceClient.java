package com.mockly.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockly.core.dto.ml.MLProcessRequest;
import com.mockly.core.dto.ml.MLProcessResponse;
import com.mockly.core.exception.MLProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for communicating with ML service.
 * Handles requests to process artifacts and generate reports.
 */
@Service
@Slf4j
public class MLServiceClient {

    private final WebClient mlServiceWebClient;
    private final ObjectMapper objectMapper;

    public MLServiceClient(@Qualifier("mlServiceWebClient") WebClient mlServiceWebClient, ObjectMapper objectMapper) {
        this.mlServiceWebClient = mlServiceWebClient;
        this.objectMapper = objectMapper;
    }

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Process artifact via ML service.
     * POST /api/process to ML service with configurable base URL (ML_SERVICE_URL).
     *
     * @param request Processing request with artifact details
     * @return ML processing response with metrics, summary, recommendations, and transcript
     * @throws MLProcessingException on failure
     */
    public MLProcessResponse process(MLProcessRequest request) {
        log.info("Sending request to ML service: sessionId={}, artifactId={}, artifactType={}", 
                request.sessionId(), request.artifactId(), request.artifactType());

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("ML service request payload: {}", requestJson);

            MLProcessResponse response = mlServiceWebClient.post()
                    .uri("/api/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MLProcessResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                log.error("ML service returned null response for session: {}", request.sessionId());
                throw new MLProcessingException("ML service returned null response");
            }


            log.info("ML service processing completed successfully for session: {}", request.sessionId());
            log.debug("ML service response: metrics={}, summary length={}, recommendations length={}, transcript present={}", 
                    response.metrics() != null ? response.metrics().size() : 0,
                    response.summary() != null ? response.summary().length() : 0,
                    response.recommendations() != null ? response.recommendations().length() : 0,
                    response.transcript() != null && !response.transcript().isEmpty());

            return response;
        } catch (WebClientResponseException e) {
            log.error("ML service HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MLProcessingException(
                    String.format("ML service processing failed with status %d: %s", 
                            e.getStatusCode().value(), e.getMessage()), e);
        } catch (Exception e) {

            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("Timeout"))) {
                log.error("ML service request timeout for session: {} after {}", request.sessionId(), REQUEST_TIMEOUT, e);
                throw new MLProcessingException("ML service request timed out after " + REQUEST_TIMEOUT, e);
            }
            log.error("Unexpected error calling ML service for session: {}", request.sessionId(), e);
            throw new MLProcessingException("Failed to process artifact with ML service: " + 
                    (errorMessage != null ? errorMessage : e.getClass().getSimpleName()), e);
        }
    }

    /**
     * @deprecated Use {@link #process(MLProcessRequest)} instead
     */
    @Deprecated
    public MLProcessResponse processArtifact(MLProcessRequest request) {
        return process(request);
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

