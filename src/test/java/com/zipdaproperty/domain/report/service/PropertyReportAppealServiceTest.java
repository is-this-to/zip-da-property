package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.repository.PropertyReportActionRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportAppealRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportAppealCreateResponse;
import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportAppealServiceTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long AUTHOR_MEMBER_ID = 2001L;
    private static final Long ACTION_ADMIN_ID = 3001L;
    private static final Long APPEAL_ID = 41L;
    private static final String DETAIL = "운영조치에 이의를 신청하는 상세 사유입니다.";

    private final PropertyReportRepository reportRepository =
            mock(PropertyReportRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyReportAppealRepository appealRepository =
            mock(PropertyReportAppealRepository.class);
    private final PropertyReportActionRepository actionRepository =
            mock(PropertyReportActionRepository.class);
    private final PropertyReportAppealService service =
            new PropertyReportAppealService(
                    reportRepository,
                    propertyRepository,
                    appealRepository,
                    actionRepository
            );

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void createAppeal_propertyAuthor_savesSubmittedAndReturnsCurrentReport(
            ActorRole role
    ) {
        ActorContext actorContext = actor(AUTHOR_MEMBER_ID, role);
        PropertyReport report = prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);
        prepareAllowedAction(Instant.now().minus(6, ChronoUnit.DAYS));
        when(appealRepository.saveAndFlush(any(PropertyReportAppeal.class)))
                .thenAnswer(invocation -> {
                    PropertyReportAppeal appeal = invocation.getArgument(0);
                    ReflectionTestUtils.setField(appeal, "appealId", APPEAL_ID);
                    return appeal;
                });

        PropertyReportAppealCreateResponse response = service.createAppeal(
                REPORT_ID,
                DETAIL,
                actorContext
        );

        ArgumentCaptor<PropertyReportAppeal> captor =
                ArgumentCaptor.forClass(PropertyReportAppeal.class);
        verify(appealRepository).saveAndFlush(captor.capture());
        PropertyReportAppeal appeal = captor.getValue();
        assertThat(appeal.getReportId()).isEqualTo(REPORT_ID);
        assertThat(appeal.getAppellantMemberId()).isEqualTo(AUTHOR_MEMBER_ID);
        assertThat(appeal.getDetail()).isEqualTo(DETAIL);
        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.SUBMITTED);
        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.appealId()).isEqualTo(APPEAL_ID);
        assertThat(response.status()).isEqualTo(ReportStatus.ACTIONED);
        assertThat(response.version()).isEqualTo(3L);
        verify(report, never()).changeStatus(any(), any());
    }

    @Test
    void createAppeal_missingReport_throwsNotFound() {
        when(reportRepository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
        );

        verifyNoInteractions(propertyRepository, appealRepository);
    }

    @Test
    void createAppeal_missingProperty_throwsPropertyNotFound() {
        prepareReport();
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.PROPERTY_NOT_FOUND)
        );

        verifyNoInteractions(appealRepository);
    }

    @Test
    void createAppeal_otherMember_throwsOwnershipRequired() {
        prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(9999L, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED)
        );

        verifyNoInteractions(appealRepository);
    }

    @Test
    void createAppeal_withoutAllowedAction_throwsAppealNotAllowed() {
        prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);
        when(actionRepository
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        REPORT_ID,
                        allowedActionCodes()
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.APPEAL_NOT_ALLOWED)
        );

        verifyNoInteractions(appealRepository);
    }

    @Test
    void createAppeal_latestAllowedActionOlderThanSevenDays_throwsExpired() {
        prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);
        prepareAllowedAction(Instant.now().minus(8, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.APPEAL_PERIOD_EXPIRED)
        );

        verifyNoInteractions(appealRepository);
    }

    @Test
    void createAppeal_existingAppeal_throwsDuplicateAppeal() {
        prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);
        prepareAllowedAction(Instant.now().minus(1, ChronoUnit.DAYS));
        when(appealRepository.existsByReportId(REPORT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.DUPLICATE_APPEAL)
        );

        verify(appealRepository, never()).saveAndFlush(any());
    }

    @Test
    void createAppeal_uniqueConflict_throwsDuplicateAppeal() {
        prepareReport();
        prepareProperty(AUTHOR_MEMBER_ID);
        prepareAllowedAction(Instant.now().minus(1, ChronoUnit.DAYS));
        when(appealRepository.existsByReportId(REPORT_ID)).thenReturn(false);
        when(appealRepository.saveAndFlush(any(PropertyReportAppeal.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key uq_property_report_appeal_report"
                ));

        assertThatThrownBy(() -> service.createAppeal(
                REPORT_ID,
                DETAIL,
                actor(AUTHOR_MEMBER_ID, ActorRole.USER)
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.DUPLICATE_APPEAL)
        );
    }

    private PropertyReport prepareReport() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getReportId()).thenReturn(REPORT_ID);
        when(report.getPropertyId()).thenReturn(PROPERTY_ID);
        when(report.getStatus()).thenReturn(ReportStatus.ACTIONED);
        when(report.getVersion()).thenReturn(3L);
        when(reportRepository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.of(report));
        return report;
    }

    private void prepareProperty(Long authorMemberId) {
        Property property = mock(Property.class);
        when(property.getAuthorMemberId()).thenReturn(authorMemberId);
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
    }

    private void prepareAllowedAction(Instant executedAt) {
        PropertyReportAction action = PropertyReportAction.create(
                REPORT_ID,
                PROPERTY_ID,
                ReportActionCode.HIDE_PROPERTY,
                "운영조치 사유",
                executedAt,
                actor(ACTION_ADMIN_ID, ActorRole.CS_ADMIN)
        );
        when(actionRepository
                .findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
                        REPORT_ID,
                        allowedActionCodes()
                )).thenReturn(Optional.of(action));
    }

    private List<ReportActionCode> allowedActionCodes() {
        return List.of(
                ReportActionCode.HIDE_PROPERTY,
                ReportActionCode.REQUEST_CORRECTION
        );
    }

    private ActorContext actor(Long memberId, ActorRole role) {
        return ActorContext.member(memberId, role, "appeal-service-test");
    }
}
