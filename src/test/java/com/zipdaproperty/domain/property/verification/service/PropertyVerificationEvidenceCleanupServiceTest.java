package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationEvidenceRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationEvidenceCleanupServiceTest {

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-09-11T09:00:00Z");
    private static final Long FILE_ID = 3001L;

    private final PropertyVerificationEvidenceRepository evidenceRepository =
            mock(PropertyVerificationEvidenceRepository.class);
    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final PropertyFileObjectDeletionPublisher deletionPublisher =
            mock(PropertyFileObjectDeletionPublisher.class);
    private final PropertyVerificationEvidenceCleanupService service =
            new PropertyVerificationEvidenceCleanupService(
                    evidenceRepository,
                    propertyFileRepository,
                    deletionPublisher
            );

    @Test
    void cleanupExpiredEvidence_deletesEvidenceAndUnreferencedFile() {
        PropertyVerificationEvidence evidence = evidence(2001L, FILE_ID);
        PropertyFile file = linkedVerificationFile(FILE_ID);
        when(evidenceRepository.findDueForCleanup(
                List.of(
                        PropertyVerificationStatus.VERIFIED,
                        PropertyVerificationStatus.REJECTED,
                        PropertyVerificationStatus.EXPIRED
                ),
                CURRENT_TIME.minus(
                        PropertyVerificationEvidenceCleanupService
                                .EVIDENCE_RETENTION
                ),
                PageRequest.of(
                        0,
                        PropertyVerificationEvidenceCleanupService
                                .CLEANUP_BATCH_SIZE
                )
        )).thenReturn(List.of(evidence));
        when(evidenceRepository.findActivelyReferencedFileIds(
                java.util.Set.of(FILE_ID)
        )).thenReturn(List.of());
        when(propertyFileRepository
                .findAllForVerificationEvidenceCleanup(
                        java.util.Set.of(FILE_ID)
                ))
                .thenReturn(List.of(file));

        int cleanedCount = service.cleanupExpiredEvidence(CURRENT_TIME);

        assertThat(cleanedCount).isEqualTo(1);
        assertThat(evidence.getDeletedAt()).isEqualTo(CURRENT_TIME);
        assertThat(file.getDeletedAt()).isEqualTo(CURRENT_TIME);
        verify(evidenceRepository).saveAllAndFlush(List.of(evidence));
        verify(propertyFileRepository).saveAllAndFlush(List.of(file));
        verify(deletionPublisher).publishAfterCommit(List.of(file));
    }

    @Test
    void cleanupExpiredEvidence_activeSharedReferencePreservesFile() {
        PropertyVerificationEvidence evidence = evidence(2001L, FILE_ID);
        PropertyFile file = linkedVerificationFile(FILE_ID);
        when(evidenceRepository.findDueForCleanup(
                List.of(
                        PropertyVerificationStatus.VERIFIED,
                        PropertyVerificationStatus.REJECTED,
                        PropertyVerificationStatus.EXPIRED
                ),
                CURRENT_TIME.minus(
                        PropertyVerificationEvidenceCleanupService
                                .EVIDENCE_RETENTION
                ),
                PageRequest.of(
                        0,
                        PropertyVerificationEvidenceCleanupService
                                .CLEANUP_BATCH_SIZE
                )
        )).thenReturn(List.of(evidence));
        when(evidenceRepository.findActivelyReferencedFileIds(
                java.util.Set.of(FILE_ID)
        )).thenReturn(List.of(FILE_ID));
        when(propertyFileRepository
                .findAllForVerificationEvidenceCleanup(
                        java.util.Set.of(FILE_ID)
                ))
                .thenReturn(List.of(file));

        int cleanedCount = service.cleanupExpiredEvidence(CURRENT_TIME);

        assertThat(cleanedCount).isEqualTo(1);
        assertThat(evidence.getDeletedAt()).isEqualTo(CURRENT_TIME);
        assertThat(file.getDeletedAt()).isNull();
        verify(propertyFileRepository, never()).saveAllAndFlush(List.of());
        verify(deletionPublisher, never()).publishAfterCommit(List.of());
    }

    @Test
    void cleanupExpiredEvidence_withoutDueEvidenceDoesNothing() {
        when(evidenceRepository.findDueForCleanup(
                List.of(
                        PropertyVerificationStatus.VERIFIED,
                        PropertyVerificationStatus.REJECTED,
                        PropertyVerificationStatus.EXPIRED
                ),
                CURRENT_TIME.minus(
                        PropertyVerificationEvidenceCleanupService
                                .EVIDENCE_RETENTION
                ),
                PageRequest.of(
                        0,
                        PropertyVerificationEvidenceCleanupService
                                .CLEANUP_BATCH_SIZE
                )
        )).thenReturn(List.of());

        int cleanedCount = service.cleanupExpiredEvidence(CURRENT_TIME);

        assertThat(cleanedCount).isZero();
        verify(evidenceRepository, never())
                .findActivelyReferencedFileIds(java.util.Set.of(FILE_ID));
        verify(deletionPublisher, never()).publishAfterCommit(List.of());
    }

    private PropertyVerificationEvidence evidence(
            Long evidenceId,
            Long fileId
    ) {
        return new PropertyVerificationEvidence(
                evidenceId,
                1001L,
                fileId,
                PropertyVerificationEvidenceType.REGISTRY_DOCUMENT,
                0,
                memberActor()
        );
    }

    private PropertyFile linkedVerificationFile(Long fileId) {
        PropertyFile file = PropertyFile.create(
                fileId,
                "verification-cleanup-session",
                FilePurpose.VERIFICATION,
                "verification.pdf",
                1024L,
                "property-files/verification/" + fileId,
                CURRENT_TIME.plusSeconds(300),
                memberActor()
        );
        file.complete("a".repeat(64), "application/pdf", memberActor());
        file.markLinked(memberActor());
        return file;
    }

    private ActorContext memberActor() {
        return ActorContext.member(
                4001L,
                ActorRole.USER,
                "verification-evidence-cleanup-test"
        );
    }
}
