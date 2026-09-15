package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.request.PropertyReportAdminStatusChangeRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminStatusChangeResponse;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;

import static com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode.PROPERTY_REPORT_STATUS_CHANGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportAdminStatusChangeServiceTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long CURRENT_VERSION = 3L;

    private final PropertyReportRepository repository = mock(PropertyReportRepository.class);
    private final PropertyReportAdminStatusTransitionPolicy transitionPolicy =
            new PropertyReportAdminStatusTransitionPolicy();
    private final PropertyAuditEventRecorder auditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyReportAdminStatusChangeService service =
            new PropertyReportAdminStatusChangeService(
                    repository,
                    transitionPolicy,
                    auditEventRecorder
            );

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"CS_ADMIN", "SUPER_ADMIN"})
    void changeStatus_adminRoles_saveAndReturnChangedStatus(ActorRole role) {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getReportId()).thenReturn(REPORT_ID);
        when(report.getVersion()).thenReturn(CURRENT_VERSION, CURRENT_VERSION + 1);
        when(report.getStatus()).thenReturn(ReportStatus.RECEIVED, ReportStatus.TRIAGED);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.of(report));
        when(repository.saveAndFlush(report)).thenReturn(report);
        ActorContext context = ActorContext.member(3001L, role, "report-admin-status-test");

        PropertyReportAdminStatusChangeResponse response = service.changeStatus(
                REPORT_ID,
                new PropertyReportAdminStatusChangeRequest(
                        ReportStatus.TRIAGED,
                        CURRENT_VERSION
                ),
                context
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.status()).isEqualTo(ReportStatus.TRIAGED);
        assertThat(response.version()).isEqualTo(CURRENT_VERSION + 1);
        verify(report).changeStatus(ReportStatus.TRIAGED, context);
        verify(auditEventRecorder).recordAction(
                eq("PROPERTY_REPORT"),
                eq(REPORT_ID.toString()),
                eq(PROPERTY_REPORT_STATUS_CHANGED),
                isNull(),
                isNull(),
                any(Instant.class),
                eq(context)
        );
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void changeStatus_nonAdminRoles_throwForbidden(ActorRole role) {
        ActorContext context = ActorContext.member(1001L, role, "report-admin-status-test");

        assertThatThrownBy(() -> service.changeStatus(
                REPORT_ID,
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, CURRENT_VERSION),
                context
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.FORBIDDEN));
        verifyNoInteractions(repository, auditEventRecorder);
    }

    @Test
    void changeStatus_nonExistingOrSoftDeletedReport_throwsNotFound() {
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(
                REPORT_ID,
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, CURRENT_VERSION),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE));
        verifyNoInteractions(auditEventRecorder);
    }

    @Test
    void changeStatus_versionMismatch_throwsVersionConflictBeforeTransition() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getVersion()).thenReturn(CURRENT_VERSION);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.changeStatus(
                REPORT_ID,
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, 2L),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.VERSION_CONFLICT));
        verify(report, never()).changeStatus(any(), any());
        verifyNoInteractions(auditEventRecorder);
    }

    @Test
    void changeStatus_optimisticLockFailure_throwsVersionConflict() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getVersion()).thenReturn(CURRENT_VERSION);
        when(report.getStatus()).thenReturn(ReportStatus.RECEIVED);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.of(report));
        when(repository.saveAndFlush(report)).thenThrow(new OptimisticLockingFailureException("conflict"));

        assertThatThrownBy(() -> service.changeStatus(
                REPORT_ID,
                new PropertyReportAdminStatusChangeRequest(ReportStatus.TRIAGED, CURRENT_VERSION),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.VERSION_CONFLICT));
        verifyNoInteractions(auditEventRecorder);
    }

    private ActorContext adminContext() {
        return ActorContext.member(
                3001L,
                ActorRole.CS_ADMIN,
                "report-admin-status-test"
        );
    }
}
