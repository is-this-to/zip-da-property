package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyFileCleanupService {

    static final Duration ORPHAN_RETENTION = Duration.ofHours(24);
    private static final String ORPHAN_DELETE_REASON =
            "VERIFIED 후 24시간 동안 연결되지 않아 정리되었습니다.";

    private final PropertyFileRepository propertyFileRepository;
    private final PropertyFileObjectDeletionPublisher deletionPublisher;

    @Transactional
    public int cleanupVerifiedOrphans(Instant currentTime) {
        List<PropertyFile> orphanFiles = propertyFileRepository
                .findUnlinkedFilesVerifiedBefore(
                        UploadStatus.VERIFIED,
                        currentTime.minus(ORPHAN_RETENTION)
                );
        ActorContext actorContext = ActorContext.batch(
                "property-file-orphan-cleanup"
        );
        for (PropertyFile orphanFile : orphanFiles) {
            orphanFile.softDelete(
                    actorContext,
                    currentTime,
                    ORPHAN_DELETE_REASON
            );
        }
        deletionPublisher.publishAfterCommit(orphanFiles);
        return orphanFiles.size();
    }

    @Transactional
    public int retryPendingObjectDeletions() {
        List<PropertyFile> pendingFiles = propertyFileRepository
                .findAllByDeletedAtIsNotNullAndObjectDeletedAtIsNull();
        deletionPublisher.publishAfterCommit(pendingFiles);
        return pendingFiles.size();
    }
}
