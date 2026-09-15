package com.zipdaproperty.domain.property.idempotency.service;

import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import com.zipdaproperty.domain.property.idempotency.repository.PropertyIdempotencyRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PropertyIdempotencyCleanupServiceTest {

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-09-12T03:00:00Z");

    private final PropertyIdempotencyRepository repository =
            mock(PropertyIdempotencyRepository.class);
    private final PropertyIdempotencyCleanupService service =
            new PropertyIdempotencyCleanupService(repository);

    @Test
    void cleanupExpiredRecords_softDeletesOnlySelectedBatch() {
        PropertyIdempotency first = expiredRecord("key-1");
        PropertyIdempotency second = expiredRecord("key-2");
        List<PropertyIdempotency> expiredRecords = List.of(first, second);

        when(repository.findDueForCleanup(
                CURRENT_TIME,
                PageRequest.of(
                        0,
                        PropertyIdempotencyCleanupService.CLEANUP_BATCH_SIZE
                )
        )).thenReturn(expiredRecords);

        int cleanedCount = service.cleanupExpiredRecords(CURRENT_TIME);

        assertThat(cleanedCount).isEqualTo(2);
        assertThat(first.isDeleted()).isTrue();
        assertThat(second.isDeleted()).isTrue();
        assertThat(first.getDeletedAt()).isEqualTo(CURRENT_TIME);
        assertThat(first.getDeleteReason()).contains("보존 기간");
        assertThat(first.getActionSource().name()).isEqualTo("BATCH");
        verify(repository).saveAllAndFlush(expiredRecords);
    }

    @Test
    void cleanupExpiredRecords_withoutDueRecordsDoesNothing() {
        when(repository.findDueForCleanup(
                CURRENT_TIME,
                PageRequest.of(
                        0,
                        PropertyIdempotencyCleanupService.CLEANUP_BATCH_SIZE
                )
        )).thenReturn(List.of());

        int cleanedCount = service.cleanupExpiredRecords(CURRENT_TIME);

        assertThat(cleanedCount).isZero();
        verify(repository).findDueForCleanup(
                CURRENT_TIME,
                PageRequest.of(
                        0,
                        PropertyIdempotencyCleanupService.CLEANUP_BATCH_SIZE
                )
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    void cleanupExpiredRecords_withoutCurrentTimeFailsFast() {
        assertThatThrownBy(() -> service.cleanupExpiredRecords(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("멱등 기록 정리 기준 시각은 필수입니다.");

        verifyNoMoreInteractions(repository);
    }

    private PropertyIdempotency expiredRecord(String key) {
        return PropertyIdempotency.start(
                1001L,
                "POST:/api/property/properties",
                key,
                "a".repeat(64),
                CURRENT_TIME.minusSeconds(1),
                ActorContext.member(
                        1001L,
                        ActorRole.USER,
                        "cleanup-test"
                )
        );
    }
}
