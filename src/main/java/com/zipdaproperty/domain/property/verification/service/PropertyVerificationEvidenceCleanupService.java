package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionPublisher;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationEvidenceRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyVerificationEvidenceCleanupService {

    static final Duration EVIDENCE_RETENTION = Duration.ofDays(30);
    static final int CLEANUP_BATCH_SIZE = 100;
    private static final List<PropertyVerificationStatus> TERMINAL_STATUSES =
            List.of(
                    PropertyVerificationStatus.VERIFIED,
                    PropertyVerificationStatus.REJECTED,
                    PropertyVerificationStatus.EXPIRED
            );
    private static final String DELETE_REASON =
            "검증 종료 후 30일 보관 기간이 지나 정리되었습니다.";

    private final PropertyVerificationEvidenceRepository evidenceRepository;
    private final PropertyFileRepository propertyFileRepository;
    private final PropertyFileObjectDeletionPublisher deletionPublisher;

    @Transactional
    public int cleanupExpiredEvidence(Instant currentTime) {
        Instant requiredCurrentTime = Objects.requireNonNull(
                currentTime,
                "증빙 정리 기준 시각은 필수입니다."
        );
        List<PropertyVerificationEvidence> dueEvidence = evidenceRepository
                .findDueForCleanup(
                        TERMINAL_STATUSES,
                        requiredCurrentTime.minus(EVIDENCE_RETENTION),
                        PageRequest.of(0, CLEANUP_BATCH_SIZE)
                );
        if (dueEvidence.isEmpty()) {
            return 0;
        }

        ActorContext actorContext = ActorContext.batch(
                "property-verification-evidence-cleanup-"
                        + requiredCurrentTime.toEpochMilli()
        );
        dueEvidence.forEach(evidence -> evidence.softDelete(
                actorContext,
                requiredCurrentTime,
                DELETE_REASON
        ));
        evidenceRepository.saveAllAndFlush(dueEvidence);

        Set<Long> candidateFileIds = dueEvidence.stream()
                .map(PropertyVerificationEvidence::getPropertyFileId)
                .collect(Collectors.toSet());
        List<PropertyFile> candidateFiles = propertyFileRepository
                .findAllForVerificationEvidenceCleanup(candidateFileIds);
        Set<Long> activelyReferencedFileIds = new HashSet<>(
                evidenceRepository.findActivelyReferencedFileIds(
                        candidateFileIds
                )
        );
        List<PropertyFile> filesToDelete = candidateFiles.stream()
                .filter(file -> file.getFilePurpose() == FilePurpose.VERIFICATION)
                .filter(file -> !activelyReferencedFileIds.contains(
                        file.getPropertyFileId()
                ))
                .toList();

        if (filesToDelete.isEmpty()) {
            return dueEvidence.size();
        }

        filesToDelete.forEach(file -> file.softDelete(
                actorContext,
                requiredCurrentTime,
                DELETE_REASON
        ));
        propertyFileRepository.saveAllAndFlush(filesToDelete);
        deletionPublisher.publishAfterCommit(filesToDelete);

        return dueEvidence.size();
    }
}
