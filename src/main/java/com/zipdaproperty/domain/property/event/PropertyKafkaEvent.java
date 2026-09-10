package com.zipdaproperty.domain.property.event;

import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record PropertyKafkaEvent(

        @TsidString
        Long eventId,

        String eventType,

        Integer schemaVersion,

        String aggregateType,

        @TsidString
        Long aggregateId,

        Long aggregateVersion,

        Instant occurredAt,

        String traceId,

        @TsidString
        Long actorMemberId,

        String actorRole,

        Map<String, Object> payload

) {

    public PropertyKafkaEvent {
        validatePositiveId(eventId, "Kafka eventId는 0보다 커야 합니다.");
        eventType = requireText(eventType, "Kafka eventType은 필수입니다.");

        if (schemaVersion == null || schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "Kafka schemaVersion은 1 이상이어야 합니다."
            );
        }

        aggregateType = requireText(
                aggregateType,
                "Kafka aggregateType은 필수입니다."
        );
        validatePositiveId(
                aggregateId,
                "Kafka aggregateId는 0보다 커야 합니다."
        );

        if (aggregateVersion == null || aggregateVersion < 0) {
            throw new IllegalArgumentException(
                    "Kafka aggregateVersion은 0 이상이어야 합니다."
            );
        }

        occurredAt = Objects.requireNonNull(
                occurredAt,
                "Kafka occurredAt은 필수입니다."
        );
        traceId = requireText(traceId, "Kafka traceId는 필수입니다.");
        actorRole = requireText(actorRole, "Kafka actorRole은 필수입니다.");

        Map<String, Object> requiredPayload =
                Objects.requireNonNull(
                        payload,
                        "Kafka payload는 필수입니다."
                );

        payload = Collections.unmodifiableMap(
                new LinkedHashMap<>(requiredPayload)
        );
    }

    private static void validatePositiveId(
            Long value,
            String message
    ) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}
