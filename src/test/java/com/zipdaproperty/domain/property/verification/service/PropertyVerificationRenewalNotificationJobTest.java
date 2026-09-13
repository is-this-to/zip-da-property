package com.zipdaproperty.domain.property.verification.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PropertyVerificationRenewalNotificationJobTest {

    private final PropertyVerificationRenewalNotificationService service =
            mock(PropertyVerificationRenewalNotificationService.class);
    private final PropertyVerificationRenewalNotificationJob job =
            new PropertyVerificationRenewalNotificationJob(service);

    @Test
    void run_notifiesDueRenewalsAtCurrentTime() {
        job.run();

        verify(service).notifyDueRenewals(any(Instant.class));
    }
}
