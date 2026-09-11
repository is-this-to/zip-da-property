package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyFileCleanupServiceTest {

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyFileObjectDeletionPublisher deletionPublisher =
            mock(PropertyFileObjectDeletionPublisher.class);
    private final PropertyFileCleanupService service =
            new PropertyFileCleanupService(
                    propertyFileRepository,
                    deletionPublisher
            );

    @Test
    void cleanupVerifiedOrphans_usesVerifiedUpdatedAtTwentyFourHourCutoff() {
        Instant currentTime = Instant.parse("2026-09-11T03:00:00Z");
        Instant verifiedBefore = currentTime.minus(
                PropertyFileCleanupService.ORPHAN_RETENTION
        );
        PropertyFile orphan = verifiedFile(Instant.now());
        when(propertyFileRepository.findUnlinkedFilesVerifiedBefore(
                UploadStatus.VERIFIED,
                verifiedBefore
        )).thenReturn(List.of(orphan));

        int cleanedCount = service.cleanupVerifiedOrphans(currentTime);

        assertThat(cleanedCount).isEqualTo(1);
        assertThat(orphan.getDeletedAt()).isEqualTo(currentTime);
        verify(deletionPublisher).publishAfterCommit(List.of(orphan));
    }

    @Test
    void retryPendingObjectDeletions_reschedulesRowsWithoutObjectDeletedAt() {
        PropertyFile pending = verifiedFile(Instant.now());
        pending.softDelete(
                ActorContext.batch("pending-file"),
                Instant.now(),
                "pending"
        );
        when(propertyFileRepository
                .findAllByDeletedAtIsNotNullAndObjectDeletedAtIsNull())
                .thenReturn(List.of(pending));

        int retryCount = service.retryPendingObjectDeletions();

        assertThat(retryCount).isEqualTo(1);
        verify(deletionPublisher).publishAfterCommit(List.of(pending));
    }

    private PropertyFile verifiedFile(Instant expiresAt) {
        ActorContext actorContext = ActorContext.member(
                1001L,
                ActorRole.USER,
                "orphan-file-test"
        );
        PropertyFile propertyFile = PropertyFile.create(
                101L,
                "upload-session",
                FilePurpose.PROPERTY_IMAGE,
                "photo.jpg",
                1024L,
                "property-files/test/101.jpg",
                expiresAt,
                actorContext
        );
        propertyFile.complete(
                "a".repeat(64),
                "image/jpeg",
                actorContext
        );
        return propertyFile;
    }
}
