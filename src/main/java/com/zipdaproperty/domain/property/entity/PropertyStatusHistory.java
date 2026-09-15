package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyStatusHistory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "property_status_history_id",
            nullable = false,
            updatable = false
    )
    private Long propertyStatusHistoryId;

    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Column(
            name = "property_revision_id",
            nullable = false,
            updatable = false
    )
    private Long propertyRevisionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status_type",
            nullable = false,
            updatable = false,
            length = 30
    )
    private PropertyStatusType statusType;

    @Column(
            name = "before_status",
            updatable = false,
            length = 30
    )
    private String beforeStatus;

    @Column(
            name = "after_status",
            nullable = false,
            updatable = false,
            length = 30
    )
    private String afterStatus;

    @Column(
            name = "actor_member_id",
            updatable = false
    )
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_role",
            updatable = false,
            length = 30
    )
    private ActorRole actorRole;

    @Column(
            name = "reason_code",
            updatable = false,
            length = 50
    )
    private String reasonCode;

    @Column(
            name = "reason",
            updatable = false,
            length = 1000
    )
    private String reason;

    @Column(
            name = "property_version",
            nullable = false,
            updatable = false
    )
    private Long propertyVersion;

    @Column(
            name = "trace_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String traceId;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant occurredAt; // 실제 업무 사건이 발생한 시각

    public PropertyStatusHistory(
            Long propertyId,
            Long propertyRevisionId,
            PropertyStatusType statusType,
            String beforeStatus,
            String afterStatus,
            String reasonCode,
            String reason,
            Long propertyVersion,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyId = propertyId;
        this.propertyRevisionId = propertyRevisionId;
        this.statusType = statusType;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.actorMemberId = actorContext.memberId();
        this.actorRole = actorContext.role();
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.propertyVersion = propertyVersion;
        this.traceId = actorContext.traceId();
        this.occurredAt = occurredAt;
    }
}