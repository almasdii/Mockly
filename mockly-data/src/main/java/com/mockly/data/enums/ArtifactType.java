package com.mockly.data.enums;

/**
 * Type of artifact generated from a session.
 */
public enum ArtifactType {
    /**
     * Mixed audio recording (both participants).
     */
    AUDIO_MIXED,

    /**
     * Audio recording of left channel (typically candidate).
     */
    AUDIO_LEFT,

    /**
     * Audio recording of right channel (typically interviewer).
     */
    AUDIO_RIGHT,

    /**
     * Raw WebRTC recording data.
     */
    RAW_WEBRTC
}

