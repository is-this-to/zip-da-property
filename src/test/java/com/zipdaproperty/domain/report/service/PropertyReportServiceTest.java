package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.response.PropertyReportCreateResponse;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportServiceTest {

    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long REPORT_ID = 884700000000000002L;
    private static final Long REPORTER_MEMBER_ID = 1001L;
    private static final Long INITIAL_VERSION = 0L;
    private static final String DETAIL = "허위 매물 정보가 포함되어 있어 신고합니다.";

    private final PropertyReportRepository propertyReportRepository =
            mock(PropertyReportRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);

    private PropertyReportService service;

    @BeforeEach
    void setUp() {
        service = new PropertyReportService(
                propertyReportRepository,
                propertyRepository,
                tsidGenerator
        );

        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(mock(Property.class)));
        when(propertyReportRepository.countDailyReportsIncludingDeleted(
                REPORTER_MEMBER_ID
        )).thenReturn(0L);
        when(propertyReportRepository
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                )).thenReturn(false);
        when(tsidGenerator.generate()).thenReturn(REPORT_ID);
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenAnswer(invocation -> {
                    PropertyReport report = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            report,
                            "version",
                            INITIAL_VERSION
                    );
                    return report;
                });
    }

    @Test
    void createReport_validRequest_savesReportAndReturnsCreatedReport() {
        ActorContext actorContext = actor(ActorRole.USER);

        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                actorContext
        );

        ArgumentCaptor<PropertyReport> reportCaptor =
                ArgumentCaptor.forClass(PropertyReport.class);
        verify(propertyReportRepository).saveAndFlush(reportCaptor.capture());

        PropertyReport savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReportId()).isEqualTo(REPORT_ID);
        assertThat(savedReport.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(savedReport.getReporterMemberId())
                .isEqualTo(REPORTER_MEMBER_ID);
        assertThat(savedReport.getReasonCode())
                .isEqualTo(ReportReasonCode.FALSE_INFO);
        assertThat(savedReport.getDetail()).isEqualTo(DETAIL);
        assertThat(savedReport.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(savedReport.getCreatedByMemberId())
                .isEqualTo(REPORTER_MEMBER_ID);
        assertThat(savedReport.getCreatedByRole()).isEqualTo(ActorRole.USER);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(response.version()).isEqualTo(INITIAL_VERSION);
    }

    @Test
    void createReport_propertyDoesNotExist_throwsPropertyNotFound() {
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.DUPLICATE,
                        DETAIL,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.PROPERTY_NOT_FOUND
        );

        verifyNoInteractions(propertyReportRepository, tsidGenerator);
    }

    @Test
    void createReport_sameActiveReportExists_throwsDuplicateActiveReport() {
        when(propertyReportRepository
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                )).thenReturn(true);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.UNAVAILABLE,
                        DETAIL,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.DUPLICATE_ACTIVE_REPORT
        );

        verify(propertyReportRepository, never())
                .saveAndFlush(any(PropertyReport.class));
        verifyNoInteractions(tsidGenerator);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, 3L, 4L})
    void createReport_fewerThanFiveSuccessfulReports_allowsNextReport(
            long existingDailyCount
    ) {
        when(propertyReportRepository.countDailyReportsIncludingDeleted(
                REPORTER_MEMBER_ID
        )).thenReturn(existingDailyCount);

        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.PRICE_MISMATCH,
                DETAIL,
                actor(ActorRole.USER)
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        verify(propertyReportRepository)
                .saveAndFlush(any(PropertyReport.class));
    }

    @Test
    void createReport_fiveSuccessfulReportsAlreadyExist_throwsRateLimited() {
        when(propertyReportRepository.countDailyReportsIncludingDeleted(
                REPORTER_MEMBER_ID
        )).thenReturn(5L);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.OTHER,
                        DETAIL,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.RATE_LIMITED
        );

        verify(propertyReportRepository, never())
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        anyLong(),
                        anyLong(),
                        any(ReportReasonCode.class),
                        any()
                );
        verify(propertyReportRepository, never())
                .saveAndFlush(any(PropertyReport.class));
        verifyNoInteractions(tsidGenerator);
    }

    @Test
    void createReport_activeDuplicateCheck_excludesTerminalStatuses() {
        service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                actor(ActorRole.USER)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ReportStatus>> statusesCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(propertyReportRepository)
                .existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
                        eq(REPORTER_MEMBER_ID),
                        eq(PROPERTY_ID),
                        eq(ReportReasonCode.FALSE_INFO),
                        statusesCaptor.capture()
                );

        assertThat(statusesCaptor.getValue()).containsExactly(
                ReportStatus.RECEIVED,
                ReportStatus.TRIAGED,
                ReportStatus.IN_REVIEW,
                ReportStatus.ACTIONED
        );
        assertThat(statusesCaptor.getValue()).doesNotContain(
                ReportStatus.REJECTED,
                ReportStatus.CLOSED
        );
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void createReport_userAndAgentRoles_areAllowed(ActorRole actorRole) {
        PropertyReportCreateResponse response = service.createReport(
                PROPERTY_ID,
                ReportReasonCode.OTHER,
                DETAIL,
                actor(actorRole)
        );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        verify(propertyReportRepository)
                .saveAndFlush(any(PropertyReport.class));
    }

    @Test
    void createReport_uniqueConstraintViolation_isConvertedToDuplicateActiveReport() {
        DataIntegrityViolationException uniqueViolation =
                new DataIntegrityViolationException(
                        "Duplicate entry for unique constraint uq_property_report_01"
                );
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenThrow(uniqueViolation);

        assertBusinessException(
                () -> service.createReport(
                        PROPERTY_ID,
                        ReportReasonCode.FALSE_INFO,
                        DETAIL,
                        actor(ActorRole.USER)
                ),
                CustomResponseCode.DUPLICATE_ACTIVE_REPORT
        );
    }

    @Test
    void createReport_unrelatedDataIntegrityViolation_isRethrown() {
        DataIntegrityViolationException unrelatedViolation =
                new DataIntegrityViolationException("not null constraint violation");
        when(propertyReportRepository.saveAndFlush(any(PropertyReport.class)))
                .thenThrow(unrelatedViolation);

        assertThatThrownBy(() -> service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                actor(ActorRole.USER)
        )).isSameAs(unrelatedViolation);
    }

    private ActorContext actor(ActorRole role) {
        return ActorContext.member(
                REPORTER_MEMBER_ID,
                role,
                "property-report-service-test"
        );
    }

    private void assertBusinessException(
            ThrowingCallable callable,
            CustomResponseCode expectedCode
    ) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(expectedCode)
                );
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
