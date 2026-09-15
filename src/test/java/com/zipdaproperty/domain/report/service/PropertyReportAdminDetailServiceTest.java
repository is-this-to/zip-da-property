package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.report.entity.PropertyReportEvidence;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminDetailQueryRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAdminDetailQueryRow;
import com.zipdaproperty.domain.report.repository.PropertyReportEvidenceRepository;
import com.zipdaproperty.domain.report.response.PropertyReportAdminDetailResponse;
import com.zipdaproperty.domain.report.type.ReportEvidenceType;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode.PROPERTY_REPORT_DETAIL_VIEWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportAdminDetailServiceTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-13T01:02:03.456789Z");

    private final PropertyReportAdminDetailQueryRepository repository =
            mock(PropertyReportAdminDetailQueryRepository.class);
    private final PropertyAuditEventRecorder auditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyReportEvidenceRepository evidenceRepository =
            mock(PropertyReportEvidenceRepository.class);
    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);
    private final MinioPresignedGetUrlGenerator getUrlGenerator =
            mock(MinioPresignedGetUrlGenerator.class);
    private final PropertyReportAdminDetailService service =
            new PropertyReportAdminDetailService(
                    repository,
                    evidenceRepository,
                    propertyFileRepository,
                    getUrlGenerator,
                    auditEventRecorder
            );

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"CS_ADMIN", "SUPER_ADMIN"})
    void findReport_adminRoles_mapDetailAndRecordTrimmedAuditReason(ActorRole role) {
        when(repository.findReport(REPORT_ID)).thenReturn(Optional.of(row()));
        PropertyReportEvidence evidence = PropertyReportEvidence.create(
                REPORT_ID,
                4001L,
                ReportEvidenceType.SCREENSHOT,
                0,
                adminContext()
        );
        PropertyFile file = PropertyFile.create(
                4001L,
                "report-evidence-session",
                FilePurpose.REPORT_EVIDENCE,
                "evidence.jpg",
                1024L,
                "report/evidence/4001",
                Instant.parse("2026-09-15T00:00:00Z"),
                ActorContext.member(1001L, ActorRole.USER, "report-admin-detail-test")
        );
        file.complete("a".repeat(64), "image/jpeg", adminContext());
        file.markLinked(adminContext());
        when(evidenceRepository
                .findAllByReportIdAndDeletedAtIsNullOrderBySortOrderAscReportEvidenceIdAsc(
                        REPORT_ID
                )).thenReturn(List.of(evidence));
        when(propertyFileRepository.findAllByPropertyFileIdInAndDeletedAtIsNull(
                List.of(4001L)
        )).thenReturn(List.of(file));
        when(getUrlGenerator.generate("report/evidence/4001"))
                .thenReturn("https://minio.test/report/evidence/4001");
        ActorContext context = ActorContext.member(3001L, role, "report-admin-detail-test");

        PropertyReportAdminDetailResponse response =
                service.findReport(REPORT_ID, "  고객 문의 확인  ", context);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.propertyId()).isEqualTo(2001L);
        assertThat(response.reporterMemberId()).isEqualTo(1001L);
        assertThat(response.reasonCode()).isEqualTo(ReportReasonCode.FALSE_INFO);
        assertThat(response.detail()).isEqualTo("허위 매물 신고 상세");
        assertThat(response.status()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(response.riskScore()).isEqualByComparingTo("42.50");
        assertThat(response.assignedAdminId()).isEqualTo(3001L);
        assertThat(response.version()).isEqualTo(3L);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.evidence()).singleElement().satisfies(item -> {
            assertThat(item.fileId()).isEqualTo(4001L);
            assertThat(item.evidenceType()).isEqualTo(ReportEvidenceType.SCREENSHOT);
            assertThat(item.sortOrder()).isZero();
            assertThat(item.fileUrl())
                    .isEqualTo("https://minio.test/report/evidence/4001");
        });
        verify(auditEventRecorder).recordAction(
                eq("PROPERTY_REPORT"),
                eq(REPORT_ID.toString()),
                eq(PROPERTY_REPORT_DETAIL_VIEWED),
                eq("고객 문의 확인"),
                isNull(),
                any(Instant.class),
                eq(context)
        );
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void findReport_nonAdminRoles_throwForbidden(ActorRole role) {
        ActorContext context = ActorContext.member(1001L, role, "report-admin-detail-test");

        assertThatThrownBy(() -> service.findReport(REPORT_ID, "조회 사유", context))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.FORBIDDEN));
        verifyNoInteractions(repository, auditEventRecorder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void findReport_missingOrBlankAuditReason_throwsAuditReasonRequired(String auditReason) {
        ActorContext context = adminContext();

        assertThatThrownBy(() -> service.findReport(REPORT_ID, auditReason, context))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.AUDIT_REASON_REQUIRED));
        verifyNoInteractions(repository, auditEventRecorder);
    }

    @Test
    void findReport_tooLongAuditReason_throwsInvalidRequest() {
        assertThatThrownBy(() -> service.findReport(REPORT_ID, "가".repeat(201), adminContext()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REQUEST));
        verifyNoInteractions(repository, auditEventRecorder);
    }

    @Test
    void findReport_nonExistingOrSoftDeletedReport_throwsNotFound() {
        when(repository.findReport(REPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findReport(REPORT_ID, "조회 사유", adminContext()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE));
        verifyNoInteractions(auditEventRecorder);
    }

    private ActorContext adminContext() {
        return ActorContext.member(
                3001L,
                ActorRole.CS_ADMIN,
                "report-admin-detail-test"
        );
    }

    private PropertyReportAdminDetailQueryRow row() {
        return new PropertyReportAdminDetailQueryRow(
                REPORT_ID,
                2001L,
                1001L,
                ReportReasonCode.FALSE_INFO,
                "허위 매물 신고 상세",
                ReportStatus.IN_REVIEW,
                new BigDecimal("42.50"),
                3001L,
                3L,
                CREATED_AT
        );
    }
}
