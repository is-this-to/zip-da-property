package com.zipdaproperty.domain.property.member.event;

import java.time.Instant;

public record MemberIntegrationEvent(
        String eventId,
        String eventType,
        Integer schemaVersion,
        Long memberId,
        Long agentId,
        Long agencyId,
        String sanctionScope,
        Instant occurredAt,
        String traceId
) {
    public MemberIntegrationEvent {
        eventId = requireText(eventId, "Member eventId는 필수입니다.");
        eventType = requireText(eventType, "Member eventType은 필수입니다.");

        if (eventId.length() > 100) {
            throw new IllegalArgumentException(
                    "Member eventId는 100자를 초과할 수 없습니다."
            );
        }
        if (schemaVersion == null || schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "Member schemaVersion은 1 이상이어야 합니다."
            );
        }
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(
                    "Member memberId는 0보다 커야 합니다."
            );
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "Member occurredAt은 필수입니다."
            );
        }
        traceId = requireText(traceId, "Member traceId는 필수입니다.");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
