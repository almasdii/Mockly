package com.mockly.api.websocket;

import com.mockly.core.service.ReportService;
import com.mockly.data.entity.Session;
import com.mockly.data.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for report ready events and publishes WebSocket notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventListener {

    private final SessionEventPublisher sessionEventPublisher;
    private final SessionRepository sessionRepository;

    @EventListener
    public void handleReportReady(ReportService.ReportReadyEvent event) {
        log.info("Report ready event received for session: {}", event.sessionId());

        Session session = sessionRepository.findById(event.sessionId()).orElse(null);
        if (session != null) {
            sessionEventPublisher.reportReady(session, event.report());
        } else {
            log.warn("Session not found for report ready event: {}", event.sessionId());
        }
    }
}

