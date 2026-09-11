package com.zipdaproperty.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyFileMaintenanceJob {

    private final PropertyFileCleanupService propertyFileCleanupService;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(
            fixedDelayString =
                    "${property.file-maintenance.fixed-delay-ms}"
    )
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("파일 유지보수 작업이 이미 실행 중입니다.");
            return;
        }

        try {
            int retriedCount = propertyFileCleanupService
                    .retryPendingObjectDeletions();
            int orphanCleanedCount = propertyFileCleanupService
                    .cleanupVerifiedOrphans(Instant.now());
            log.info(
                    "파일 유지보수 작업 완료. retriedCount={}, orphanCleanedCount={}",
                    retriedCount,
                    orphanCleanedCount
            );
        } finally {
            running.set(false);
        }
    }
}
