package com.zipdaproperty.domain.property.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_member_event_consumption")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEventConsumption {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "occurred_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant processedAt;

    public MemberEventConsumption(
            String eventId,
            String eventType,
            Long memberId,
            Instant occurredAt,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.memberId = memberId;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
    }
}
