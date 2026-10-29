package com.kaviyo.backend.scan;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(
        UUID id,
        UUID scanRunId,
        UUID findingId,
        String type,
        String title,
        String message,
        Instant createdAt,
        Instant readAt) {
}
