package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.global.context.ActorContext;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "property_report_appeal",
        indexes = @Index(
                name = "idx_property_report_appeal_report_status",
                columnList = "report_id, status"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyReportAppeal extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appeal_id", nullable = false, updatable = false)
    private Long appealId;

    @Column(name = "report_id", nullable = false, updatable = false)
    private Long reportId;

    @Column(
            name = "appellant_member_id",
            nullable = false,
            updatable = false
    )
    private Long appellantMemberId;

    @Column(
            name = "detail",
            nullable = false,
            updatable = false,
            length = 2000
    )
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppealStatus status;

    @Column(name = "reviewer_member_id")
    private Long reviewerMemberId;

    @Column(name = "review_reason", length = 1000)
    private String reviewReason;

    @Column(name = "reviewed_at", columnDefinition = "DATETIME(6)")
    private Instant reviewedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private PropertyReportAppeal(
            Long reportId,
            Long appellantMemberId,
            String detail,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.reportId = Objects.requireNonNull(reportId, "reportId는 필수입니다.");
        this.appellantMemberId = Objects.requireNonNull(
                appellantMemberId,
                "appellantMemberId는 필수입니다."
        );
        this.detail = Objects.requireNonNull(detail, "detail은 필수입니다.");
        this.status = AppealStatus.SUBMITTED;
    }

    public static PropertyReportAppeal create(
            Long reportId,
            Long appellantMemberId,
            String detail,
            ActorContext actorContext
    ) {
        return new PropertyReportAppeal(
                reportId,
                appellantMemberId,
                detail,
                actorContext
        );
    }
}
