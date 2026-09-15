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
@Table(name = "property_member_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyMemberState {

    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "withdrawn_at", columnDefinition = "DATETIME(6)")
    private Instant withdrawnAt;

    @Column(name = "sanction_scope", length = 100)
    private String sanctionScope;

    @Column(name = "sanctioned_at", columnDefinition = "DATETIME(6)")
    private Instant sanctionedAt;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agency_id")
    private Long agencyId;

    @Column(name = "agent_active", nullable = false)
    private boolean agentActive;

    @Column(name = "last_event_id", nullable = false, length = 100)
    private String lastEventId;

    @Column(name = "last_event_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant lastEventAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static PropertyMemberState create(
            Long memberId,
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        PropertyMemberState state = new PropertyMemberState();
        state.memberId = memberId;
        state.agentActive = false;
        state.lastEventId = eventId;
        state.lastEventAt = eventAt;
        state.createdAt = processedAt;
        state.updatedAt = processedAt;
        return state;
    }

    public boolean accepts(String eventId, Instant eventAt) {
        int timeComparison = eventAt.compareTo(lastEventAt);
        return timeComparison > 0
                || (timeComparison == 0 && eventId.compareTo(lastEventId) > 0);
    }

    public void withdraw(String eventId, Instant eventAt, Instant processedAt) {
        withdrawnAt = eventAt;
        updateCursor(eventId, eventAt, processedAt);
    }

    public void sanction(
            String scope,
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        sanctionScope = requireText(scope, "제재 범위는 필수입니다.");
        sanctionedAt = eventAt;
        updateCursor(eventId, eventAt, processedAt);
    }

    public void releaseSanction(
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        sanctionScope = null;
        sanctionedAt = null;
        updateCursor(eventId, eventAt, processedAt);
    }

    public void approveAgent(
            Long agentId,
            Long agencyId,
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        if (agentId == null || agentId <= 0 || agencyId == null || agencyId <= 0) {
            throw new IllegalArgumentException(
                    "AgentApproved 이벤트에는 유효한 agentId와 agencyId가 필요합니다."
            );
        }
        this.agentId = agentId;
        this.agencyId = agencyId;
        this.agentActive = true;
        updateCursor(eventId, eventAt, processedAt);
    }

    public void suspendAgent(
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        this.agentActive = false;
        updateCursor(eventId, eventAt, processedAt);
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }

    public boolean isSanctioned() {
        return sanctionedAt != null;
    }

    private void updateCursor(
            String eventId,
            Instant eventAt,
            Instant processedAt
    ) {
        lastEventId = eventId;
        lastEventAt = eventAt;
        updatedAt = processedAt;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
