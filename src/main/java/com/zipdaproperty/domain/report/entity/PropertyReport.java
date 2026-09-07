package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "property_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyReport extends BaseAuditEntity {

    @Id
    @Column(
            name = "report_id",
            nullable = false,
            updatable = false
    )
    private Long reportId;

    @Column(
            name = "property_id",
            nullable = false,
            updatable = false
    )
    private Long propertyId;

    @Column(
            name = "reporter_member_id",
            nullable = false,
            updatable = false
    )
    private Long reporterMemberId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "reason_code",
            nullable = false,
            length = 50
    )
    private ReportReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ReportStatus status;

    @Column(
            name = "detail",
            nullable = false,
            length = 1000
    )
    private String detail;

    @Column(
            name = "risk_score",
            precision = 5,
            scale = 2
    )
    private BigDecimal riskScore;

    @Column(name = "assigned_admin_id")
    private Long assignedAdminId;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @Column(
            name = "active_report_key",
            length = 150,
            insertable = false,
            updatable = false
    )
    private String activeReportKey;

    private PropertyReport(
            Long reportId,
            Long propertyId,
            Long reporterMemberId,
            ReportReasonCode reasonCode,
            String detail,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.reportId = reportId;
        this.propertyId = propertyId;
        this.reporterMemberId = reporterMemberId;
        this.reasonCode = reasonCode;
        this.detail = detail;
        this.status = ReportStatus.RECEIVED;
    }

    public static PropertyReport create(
            Long reportId,
            Long propertyId,
            Long reporterMemberId,
            ReportReasonCode reasonCode,
            String detail,
            ActorContext actorContext
    ) {
        return new PropertyReport(
                reportId,
                propertyId,
                reporterMemberId,
                reasonCode,
                detail,
                actorContext
        );
    }
}