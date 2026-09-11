package com.zipdaproperty.domain.property.verification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyVerificationRenewalNotificationJob {

    private final PropertyVerificationRenewalNotificationService service;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(
            fixedDelayString =
                    "${property.verification-renewal-notification.fixed-delay-ms}"
    )
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("매물 재인증 안내 작업이 이미 실행 중입니다.");
            return;
        }

        try {
            int notifiedCount = service.notifyDueRenewals(Instant.now());
            log.info(
                    "매물 재인증 안내 작업 완료. notifiedCount={}",
                    notifiedCount
            );
        } finally {
            running.set(false);
        }
    }
}
