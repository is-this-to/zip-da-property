package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.request.PropertyReportAdminRiskScoreRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminRiskScoreResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportAdminRiskScoreServiceTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final BigDecimal RISK_SCORE = new BigDecimal("42.50");
    private static final Long CURRENT_VERSION = 3L;

    private final PropertyReportRepository repository = mock(PropertyReportRepository.class);
    private final PropertyReportAdminRiskScoreService service =
            new PropertyReportAdminRiskScoreService(repository);

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"CS_ADMIN", "SUPER_ADMIN"})
    void changeRiskScore_adminRoles_changeScoreAndReturnResult(ActorRole role) {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getReportId()).thenReturn(REPORT_ID);
        when(report.getRiskScore()).thenReturn(RISK_SCORE);
        when(report.getVersion()).thenReturn(CURRENT_VERSION, CURRENT_VERSION + 1);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID)).thenReturn(Optional.of(report));
        when(repository.saveAndFlush(report)).thenReturn(report);
        ActorContext context = ActorContext.member(3001L, role, "report-risk-score-test");

        PropertyReportAdminRiskScoreResponse response = service.changeRiskScore(
                REPORT_ID,
                new PropertyReportAdminRiskScoreRequest(RISK_SCORE, CURRENT_VERSION),
                context
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.riskScore()).isEqualByComparingTo(RISK_SCORE);
        assertThat(response.version()).isEqualTo(CURRENT_VERSION + 1);
        verify(report).changeRiskScore(RISK_SCORE, context);
    }

    @Test
    void changeRiskScore_userRole_throwsForbidden() {
        ActorContext context = ActorContext.member(1001L, ActorRole.USER, "report-risk-score-test");

        assertThatThrownBy(() -> service.changeRiskScore(
                REPORT_ID,
                new PropertyReportAdminRiskScoreRequest(RISK_SCORE, CURRENT_VERSION),
                context
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.FORBIDDEN));
        verifyNoInteractions(repository);
    }

    @Test
    void changeRiskScore_missingReport_throwsNotFound() {
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRiskScore(
                REPORT_ID,
                new PropertyReportAdminRiskScoreRequest(RISK_SCORE, CURRENT_VERSION),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE));
    }

    @Test
    void changeRiskScore_versionMismatch_throwsVersionConflictBeforeChange() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getVersion()).thenReturn(CURRENT_VERSION);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.changeRiskScore(
                REPORT_ID,
                new PropertyReportAdminRiskScoreRequest(RISK_SCORE, 2L),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.VERSION_CONFLICT));
        verify(report, never()).changeRiskScore(any(), any());
    }

    @Test
    void changeRiskScore_optimisticLockFailure_throwsVersionConflict() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getVersion()).thenReturn(CURRENT_VERSION);
        when(repository.findByReportIdAndDeletedAtIsNull(REPORT_ID)).thenReturn(Optional.of(report));
        when(repository.saveAndFlush(report)).thenThrow(new OptimisticLockingFailureException("conflict"));

        assertThatThrownBy(() -> service.changeRiskScore(
                REPORT_ID,
                new PropertyReportAdminRiskScoreRequest(RISK_SCORE, CURRENT_VERSION),
                adminContext()
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.VERSION_CONFLICT));
    }

    private ActorContext adminContext() {
        return ActorContext.member(3001L, ActorRole.CS_ADMIN, "report-risk-score-test");
    }
}
