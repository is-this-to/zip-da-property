package com.zipdaproperty.domain.file.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyFileMaintenanceJobTest {

    private final PropertyFileCleanupService propertyFileCleanupService =
            mock(PropertyFileCleanupService.class);
    private final PropertyFileMaintenanceJob job =
            new PropertyFileMaintenanceJob(propertyFileCleanupService);

    @Test
    void run_retriesPendingObjectsBeforeCleaningVerifiedOrphans() {
        when(propertyFileCleanupService.retryPendingObjectDeletions())
                .thenReturn(1);
        when(propertyFileCleanupService.cleanupVerifiedOrphans(any(Instant.class)))
                .thenReturn(2);

        job.run();

        var order = inOrder(propertyFileCleanupService);
        order.verify(propertyFileCleanupService)
                .retryPendingObjectDeletions();
        order.verify(propertyFileCleanupService)
                .cleanupVerifiedOrphans(any(Instant.class));
    }
}
