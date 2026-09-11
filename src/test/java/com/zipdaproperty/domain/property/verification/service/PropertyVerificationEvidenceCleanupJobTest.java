package com.zipdaproperty.domain.property.verification.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationEvidenceCleanupJobTest {

    private final PropertyVerificationEvidenceCleanupService cleanupService =
            mock(PropertyVerificationEvidenceCleanupService.class);
    private final PropertyVerificationEvidenceCleanupJob job =
            new PropertyVerificationEvidenceCleanupJob(cleanupService);

    @Test
    void run_cleansEvidenceDueForDeletion() {
        when(cleanupService.cleanupExpiredEvidence(any(Instant.class)))
                .thenReturn(2);

        job.run();

        verify(cleanupService).cleanupExpiredEvidence(any(Instant.class));
    }
}
