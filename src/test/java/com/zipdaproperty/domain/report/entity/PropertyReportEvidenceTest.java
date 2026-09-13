package com.zipdaproperty.domain.report.entity;

import com.zipdaproperty.domain.report.type.ReportEvidenceType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportEvidenceTest {

    private static final Long REPORT_ID = 884700000000000001L;
    private static final Long FILE_ID = 884700000000000002L;
    private static final ActorContext ACTOR_CONTEXT = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-report-evidence-test"
    );

    @Test
    void create_recordsScreenshotEvidenceAndAuditData() {
        PropertyReportEvidence evidence = PropertyReportEvidence.create(
                REPORT_ID,
                FILE_ID,
                ReportEvidenceType.SCREENSHOT,
                2,
                ACTOR_CONTEXT
        );

        assertThat(evidence.getReportEvidenceId()).isNull();
        assertThat(evidence.getReportId()).isEqualTo(REPORT_ID);
        assertThat(evidence.getPropertyFileId()).isEqualTo(FILE_ID);
        assertThat(evidence.getEvidenceType())
                .isEqualTo(ReportEvidenceType.SCREENSHOT);
        assertThat(evidence.getSortOrder()).isEqualTo(2);
        assertThat(evidence.getCreatedByMemberId()).isEqualTo(1001L);
        assertThat(evidence.getCreatedByRole()).isEqualTo(ActorRole.USER);
    }

    @Test
    void mapping_usesIdentityPrimaryKeyAndReportEvidenceTable() throws Exception {
        Table table = PropertyReportEvidence.class.getAnnotation(Table.class);
        GeneratedValue generatedValue = PropertyReportEvidence.class
                .getDeclaredField("reportEvidenceId")
                .getAnnotation(GeneratedValue.class);

        assertThat(table.name()).isEqualTo("property_report_evidence");
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void softDelete_recordsDeletionAudit() {
        PropertyReportEvidence evidence = PropertyReportEvidence.create(
                REPORT_ID,
                FILE_ID,
                ReportEvidenceType.SCREENSHOT,
                0,
                ACTOR_CONTEXT
        );
        Instant deletedAt = Instant.parse("2026-09-14T12:00:00Z");

        evidence.softDelete(ACTOR_CONTEXT, deletedAt, "신고 증빙 연결 삭제");

        assertThat(evidence.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(evidence.getDeletedByMemberId()).isEqualTo(1001L);
        assertThat(evidence.getDeleteReason()).isEqualTo("신고 증빙 연결 삭제");
    }
}
