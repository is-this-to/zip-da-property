package com.zipdaproperty.domain.property.idempotency.service;

import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import com.zipdaproperty.domain.property.idempotency.repository.PropertyIdempotencyRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyIdempotencyCleanupService {

    static final int CLEANUP_BATCH_SIZE = 100;
    private static final String DELETE_REASON =
            "멱등 키 보존 기간이 만료되어 정리되었습니다.";

    private final PropertyIdempotencyRepository idempotencyRepository;

    @Transactional
    public int cleanupExpiredRecords(Instant currentTime) {
        Instant requiredCurrentTime = Objects.requireNonNull(
                currentTime,
                "멱등 기록 정리 기준 시각은 필수입니다."
        );

        List<PropertyIdempotency> expiredRecords = idempotencyRepository
                .findDueForCleanup(
                        requiredCurrentTime,
                        PageRequest.of(0, CLEANUP_BATCH_SIZE)
                );

        if (expiredRecords.isEmpty()) {
            return 0;
        }

        ActorContext actorContext = ActorContext.batch(
                "property-idempotency-cleanup-"
                        + requiredCurrentTime.toEpochMilli()
        );

        expiredRecords.forEach(record -> record.softDelete(
                actorContext,
                requiredCurrentTime,
                DELETE_REASON
        ));
        idempotencyRepository.saveAllAndFlush(expiredRecords);

        return expiredRecords.size();
    }
}
