package com.mockly.data.enums;

/**
 * Status of an interview session.
 */
public enum SessionStatus {
    /**
     * Session is scheduled for a future time.
     */
    SCHEDULED,

    /**
     * Session is currently active (in progress).
     */
    ACTIVE,

    /**
     * Session has ended successfully.
     */
    ENDED,

    /**
     * Session was canceled before it started or ended.
     */
    CANCELED
}

