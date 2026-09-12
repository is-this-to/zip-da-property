package com.zipdaproperty.domain.property.idempotency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyIdempotencyCleanupJob {

    private final PropertyIdempotencyCleanupService cleanupService;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(
            fixedDelayString =
                    "${property.idempotency-cleanup.fixed-delay-ms}"
    )
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("매물 멱등 기록 정리 작업이 이미 실행 중입니다.");
            return;
        }

        try {
            int cleanedCount = cleanupService.cleanupExpiredRecords(
                    Instant.now()
            );
            log.info(
                    "매물 멱등 기록 정리 작업 완료. cleanedCount={}",
                    cleanedCount
            );
        } finally {
            running.set(false);
        }
    }
}
