package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportEvidenceType;
import com.zipdaproperty.global.context.ActorContext;
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
@Table(name = "property_report_evidence")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyReportEvidence extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_evidence_id")
    private Long reportEvidenceId;

    @Column(name = "report_id", nullable = false, updatable = false)
    private Long reportId;

    @Column(name = "property_file_id", nullable = false, updatable = false)
    private Long propertyFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, updatable = false, length = 30)
    private ReportEvidenceType evidenceType;

    @Column(name = "sort_order", nullable = false, updatable = false)
    private Integer sortOrder;

    @Column(
            name = "active_report_file_key",
            length = 50,
            insertable = false,
            updatable = false
    )
    private String activeReportFileKey;

    private PropertyReportEvidence(
            Long reportId,
            Long propertyFileId,
            ReportEvidenceType evidenceType,
            Integer sortOrder,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.reportId = reportId;
        this.propertyFileId = propertyFileId;
        this.evidenceType = evidenceType;
        this.sortOrder = sortOrder;
    }

    public static PropertyReportEvidence create(
            Long reportId,
            Long propertyFileId,
            ReportEvidenceType evidenceType,
            Integer sortOrder,
            ActorContext actorContext
    ) {
        return new PropertyReportEvidence(
                reportId,
                propertyFileId,
                evidenceType,
                sortOrder,
                actorContext
        );
    }

    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        if (isDeleted()) {
            return;
        }
        recordDeletion(actorContext, deletedAt, deleteReason);
    }
}
