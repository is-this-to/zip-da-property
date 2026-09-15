package com.zipdaproperty.domain.property.idempotency.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyIdempotencyCleanupJobTest {

    private final PropertyIdempotencyCleanupService cleanupService =
            mock(PropertyIdempotencyCleanupService.class);
    private final PropertyIdempotencyCleanupJob job =
            new PropertyIdempotencyCleanupJob(cleanupService);

    @Test
    void run_cleansExpiredIdempotencyRecords() {
        when(cleanupService.cleanupExpiredRecords(any(Instant.class)))
                .thenReturn(2);

        job.run();

        verify(cleanupService).cleanupExpiredRecords(any(Instant.class));
    }
}
