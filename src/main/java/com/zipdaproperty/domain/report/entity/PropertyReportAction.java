package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportActionCode;
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
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Immutable
@Table(
        name = "property_report_action",
        indexes = {
                @Index(
                        name = "idx_property_report_action_report_timeline",
                        columnList = "report_id, executed_at, action_id"
                ),
                @Index(
                        name = "idx_property_report_action_property_timeline",
                        columnList = "property_id, executed_at, action_id"
                ),
                @Index(
                        name = "idx_property_report_action_actor_timeline",
                        columnList = "actor_member_id, executed_at, action_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyReportAction extends BaseAuditEntity {

    private static final int MAX_REASON_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false, updatable = false)
    private Long actionId;

    @Column(name = "report_id", nullable = false, updatable = false)
    private Long reportId;

    @Column(name = "property_id", nullable = false, updatable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action_code",
            nullable = false,
            updatable = false,
            length = 50
    )
    private ReportActionCode actionCode;

    @Column(
            name = "reason",
            nullable = false,
            updatable = false,
            length = MAX_REASON_LENGTH
    )
    private String reason;

    @Column(
            name = "actor_member_id",
            nullable = false,
            updatable = false
    )
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "actor_role",
            nullable = false,
            updatable = false,
            length = 30
    )
    private ActorRole actorRole;

    @Column(
            name = "trace_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String traceId;

    @Column(
            name = "executed_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant executedAt;

    private PropertyReportAction(
            Long reportId,
            Long propertyId,
            ReportActionCode actionCode,
            String reason,
            Instant executedAt,
            ActorContext actorContext
    ) {
        super(actorContext);

        ActorContext requiredActorContext = Objects.requireNonNull(
                actorContext,
                "신고 운영조치에는 ActorContext가 필요합니다."
        );

        this.reportId = requirePositive(reportId, "reportId");
        this.propertyId = requirePositive(propertyId, "propertyId");
        this.actionCode = Objects.requireNonNull(
                actionCode,
                "actionCode는 필수입니다."
        );
        this.reason = requireReason(reason);
        this.actorMemberId = requirePositive(
                requiredActorContext.memberId(),
                "actorMemberId"
        );
        this.actorRole = requireActorRole(requiredActorContext.role());
        this.traceId = requiredActorContext.traceId();
        this.executedAt = Objects.requireNonNull(
                executedAt,
                "executedAt은 필수입니다."
        );
    }

    public static PropertyReportAction create(
            Long reportId,
            Long propertyId,
            ReportActionCode actionCode,
            String reason,
            Instant executedAt,
            ActorContext actorContext
    ) {
        return new PropertyReportAction(
                reportId,
                propertyId,
                actionCode,
                reason,
                executedAt,
                actorContext
        );
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + "는 0보다 커야 합니다."
            );
        }
        return value;
    }

    private static ActorRole requireActorRole(ActorRole actorRole) {
        if (actorRole == null) {
            throw new IllegalArgumentException(
                    "actorRole은 필수입니다."
            );
        }
        return actorRole;
    }

    private static String requireReason(String reason) {
        if (reason == null
                || reason.isBlank()
                || reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "reason은 1자 이상 1000자 이하여야 합니다."
            );
        }
        return reason;
    }
}
