package com.zipdaproperty.domain.property.audit.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "property_audit_event",
        indexes = {
                @Index(
                        name = "idx_audit_target_timeline",
                        columnList = "target_type, target_id, occurred_at"
                ),
                @Index(
                        name = "idx_audit_actor_timeline",
                        columnList = "actor_member_id, occurred_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyAuditEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "audit_event_id",
            nullable = false,
            updatable = false
    )
    private Long auditEventId;

    @Column(
            name = "target_type",
            nullable = false,
            updatable = false,
            length = 50
    )
    private String targetType;

    @Column(
            name = "target_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String targetId;

    @Column(
            name = "action_code",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String actionCode;

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
            name = "reason",
            updatable = false,
            length = 1000
    )
    private String reason;

    @Column(
            name = "trace_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String traceId;

    @Column(
            name = "client_ip_hash",
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String clientIpHash;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant occurredAt;

    private PropertyAuditEvent(
            String targetType,
            String targetId,
            String actionCode,
            String reason,
            String clientIpHash,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        super(actorContext);

        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "감사 이벤트에는 ActorContext가 필요합니다."
        );

        this.targetType = requireText(
                targetType,
                50,
                "감사 이벤트의 targetType은 필수입니다."
        );
        this.targetId = requireText(
                targetId,
                100,
                "감사 이벤트의 targetId는 필수입니다."
        );
        this.actionCode = requireText(
                actionCode,
                100,
                "감사 이벤트의 actionCode는 필수입니다."
        );
        this.actorMemberId = requiredActorContext.memberId();
        this.actorRole = requiredActorContext.role();
        this.reason = validateOptionalText(
                reason,
                1000,
                "감사 이벤트의 reason은 1000자를 초과할 수 없습니다."
        );
        this.traceId = requireText(
                requiredActorContext.traceId(),
                100,
                "감사 이벤트의 traceId는 필수입니다."
        );
        this.clientIpHash = validateClientIpHash(clientIpHash);
        this.occurredAt = Objects.requireNonNull(
                occurredAt,
                "감사 이벤트의 발생 시각은 필수입니다."
        );
    }

    public static PropertyAuditEvent capture(
            String targetType,
            String targetId,
            String actionCode,
            String reason,
            String clientIpHash,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        return new PropertyAuditEvent(
                targetType,
                targetId,
                actionCode,
                reason,
                clientIpHash,
                occurredAt,
                actorContext
        );
    }

    private static String requireText(
            String value,
            int maximumLength,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String validateOptionalText(
            String value,
            int maximumLength,
            String message
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String validateClientIpHash(String clientIpHash) {
        if (clientIpHash == null) {
            return null;
        }

        if (clientIpHash.length() != 64) {
            throw new IllegalArgumentException(
                    "clientIpHash는 64자 SHA-256 해시여야 합니다."
            );
        }

        return clientIpHash;
    }
}