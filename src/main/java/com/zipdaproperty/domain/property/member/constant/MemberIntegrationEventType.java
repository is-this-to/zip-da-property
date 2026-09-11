package com.zipdaproperty.domain.property.member.constant;

import java.util.Locale;

public enum MemberIntegrationEventType {
    MEMBER_WITHDRAWN,
    MEMBER_SANCTIONED,
    MEMBER_SANCTION_RELEASED,
    AGENT_APPROVED,
    AGENT_SUSPENDED;

    public static MemberIntegrationEventType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Member eventType은 필수입니다.");
        }

        String normalized = value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "지원하지 않는 Member eventType입니다: " + value,
                    exception
            );
        }
    }
}
