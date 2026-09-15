package com.zipdaproperty.domain.property.verification.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PropertyVerificationExpirationJobTest {

    private final PropertyVerificationExpirationService expirationService =
            mock(PropertyVerificationExpirationService.class);
    private final PropertyVerificationExpirationJob job =
            new PropertyVerificationExpirationJob(expirationService);

    @Test
    void run_expiresDueVerificationsAtCurrentTime() {
        job.run();

        verify(expirationService)
                .expireDueVerifications(any(Instant.class));
    }
}
