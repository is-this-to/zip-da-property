package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.property.command.PropertyPublicationStatusChangeCommand;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.service.PropertyPublicationStatusChangeService;
import com.zipdaproperty.domain.report.client.MemberSanctionClient;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.repository.PropertyReportActionRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.request.PropertyReportActionRequest;
import com.zipdaproperty.domain.report.response.PropertyReportActionResponse;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyReportActionServiceTest {

    private static final Long ACTION_ID = 501L;
    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long AUTHOR_MEMBER_ID = 2001L;
    private static final String REASON = "신고 검토에 따른 운영조치";

    private final PropertyReportRepository propertyReportRepository =
            mock(PropertyReportRepository.class);
    private final PropertyReportActionRepository actionRepository =
            mock(PropertyReportActionRepository.class);
    private final PropertyPublicationStatusChangeService
            publicationStatusChangeService =
            mock(PropertyPublicationStatusChangeService.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final MemberSanctionClient memberSanctionClient =
            mock(MemberSanctionClient.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MemberSanctionClient>
            memberSanctionClientProvider =
            mock(ObjectProvider.class);

    private final PropertyReportActionService service =
            new PropertyReportActionService(
                    propertyReportRepository,
                    actionRepository,
                    publicationStatusChangeService,
                    propertyRepository,
                    memberSanctionClientProvider
            );

    private final ActorContext adminContext = ActorContext.member(
            3001L,
            ActorRole.CS_ADMIN,
            "property-report-action-service-test"
    );

    @Test
    void executeAction_hideProperty_changesPublicationThenAppendsAction() {
        PropertyReport report = prepareReport();
        prepareActionSave();

        PropertyReportActionResponse response = service.executeAction(
                REPORT_ID,
                request(ReportActionCode.HIDE_PROPERTY),
                adminContext
        );

        verify(publicationStatusChangeService).change(
                new PropertyPublicationStatusChangeCommand(
                        PROPERTY_ID,
                        PublicationStatus.HIDDEN,
                        REASON
                ),
                adminContext
        );
        verifyNoInteractions(
                propertyRepository,
                memberSanctionClientProvider,
                memberSanctionClient
        );
        assertSavedAction(ReportActionCode.HIDE_PROPERTY);
        assertResponse(response, ReportActionCode.HIDE_PROPERTY);
        verify(report, never()).changeStatus(any(), any());
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void executeAction_restoreProperty_changesPublicationThenAppendsAction() {
        PropertyReport report = prepareReport();
        prepareActionSave();

        PropertyReportActionResponse response = service.executeAction(
                REPORT_ID,
                request(ReportActionCode.RESTORE_PROPERTY),
                adminContext
        );

        verify(publicationStatusChangeService).change(
                new PropertyPublicationStatusChangeCommand(
                        PROPERTY_ID,
                        PublicationStatus.PUBLISHED,
                        REASON
                ),
                adminContext
        );
        verifyNoInteractions(
                propertyRepository,
                memberSanctionClientProvider,
                memberSanctionClient
        );
        assertSavedAction(ReportActionCode.RESTORE_PROPERTY);
        assertResponse(response, ReportActionCode.RESTORE_PROPERTY);
        verify(report, never()).changeStatus(any(), any());
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void executeAction_requestCorrection_appendsWithoutChangingProperty() {
        PropertyReport report = prepareReport();
        prepareActionSave();

        PropertyReportActionResponse response = service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_CORRECTION),
                adminContext
        );

        verifyNoInteractions(publicationStatusChangeService);
        verifyNoInteractions(
                propertyRepository,
                memberSanctionClientProvider,
                memberSanctionClient
        );
        assertSavedAction(ReportActionCode.REQUEST_CORRECTION);
        assertResponse(response, ReportActionCode.REQUEST_CORRECTION);
        verify(report, never()).changeStatus(any(), any());
        verify(propertyReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void executeAction_superAdmin_appendsAction() {
        ActorContext superAdminContext = ActorContext.member(
                3002L,
                ActorRole.SUPER_ADMIN,
                "property-report-action-super-admin-test"
        );
        prepareReport();
        prepareActionSave();

        PropertyReportActionResponse response = service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_CORRECTION),
                superAdminContext
        );

        assertThat(response.actionCode())
                .isEqualTo(ReportActionCode.REQUEST_CORRECTION);
        verify(actionRepository).save(any(PropertyReportAction.class));
        verifyNoInteractions(publicationStatusChangeService);
        verifyNoInteractions(
                propertyRepository,
                memberSanctionClientProvider,
                memberSanctionClient
        );
    }

    @ParameterizedTest
    @EnumSource(value = ActorRole.class, names = {"USER", "AGENT"})
    void executeAction_nonAdminRole_throwsForbidden(ActorRole role) {
        ActorContext actorContext = ActorContext.member(
                1001L,
                role,
                "property-report-action-forbidden-test"
        );

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.HIDE_PROPERTY),
                actorContext
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.FORBIDDEN)
        );

        verifyNoInteractions(
                propertyReportRepository,
                actionRepository,
                publicationStatusChangeService
        );
    }

    @Test
    void executeAction_missingReport_throwsNotFound() {
        when(propertyReportRepository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.HIDE_PROPERTY),
                adminContext
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
        );

        verifyNoInteractions(actionRepository, publicationStatusChangeService);
    }

    @Test
    void executeAction_propertyCommandFails_doesNotSaveAction() {
        prepareReport();
        BusinessException propertyFailure = new BusinessException(
                CustomResponseCode.INVALID_STATUS_TRANSITION,
                "공개 상태 변경 실패"
        );
        when(publicationStatusChangeService.change(
                new PropertyPublicationStatusChangeCommand(
                        PROPERTY_ID,
                        PublicationStatus.HIDDEN,
                        REASON
                ),
                adminContext
        )).thenThrow(propertyFailure);

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.HIDE_PROPERTY),
                adminContext
        )).isSameAs(propertyFailure);

        verifyNoInteractions(actionRepository);
    }

    @Test
    void executeAction_memberSanction_callsClientWithAuthorAndAppendsAction() {
        prepareReport();
        prepareProperty();
        when(memberSanctionClientProvider.getIfAvailable())
                .thenReturn(memberSanctionClient);
        prepareActionSave();

        PropertyReportActionResponse response = service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_MEMBER_SANCTION),
                adminContext
        );

        verify(propertyRepository).findById(PROPERTY_ID);
        verify(memberSanctionClient).requestSanction(
                AUTHOR_MEMBER_ID,
                REASON
        );
        assertSavedAction(ReportActionCode.REQUEST_MEMBER_SANCTION);
        assertResponse(response, ReportActionCode.REQUEST_MEMBER_SANCTION);
        verifyNoInteractions(publicationStatusChangeService);
    }

    @Test
    void executeAction_memberSanctionClientFails_doesNotSaveAction() {
        prepareReport();
        prepareProperty();
        when(memberSanctionClientProvider.getIfAvailable())
                .thenReturn(memberSanctionClient);
        BusinessException clientFailure = new BusinessException(
                CustomResponseCode.MEMBER_API_UNAVAILABLE,
                "Member 제재 요청 실패"
        );
        org.mockito.Mockito.doThrow(clientFailure)
                .when(memberSanctionClient)
                .requestSanction(AUTHOR_MEMBER_ID, REASON);

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_MEMBER_SANCTION),
                adminContext
        )).isSameAs(clientFailure);

        verify(propertyRepository).findById(PROPERTY_ID);
        verifyNoInteractions(actionRepository, publicationStatusChangeService);
    }

    @Test
    void executeAction_memberSanctionPropertyMissing_doesNotCallClientOrSaveAction() {
        prepareReport();
        when(propertyRepository.findById(PROPERTY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_MEMBER_SANCTION),
                adminContext
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.PROPERTY_NOT_FOUND)
        );

        verify(propertyRepository).findById(PROPERTY_ID);
        verifyNoInteractions(
                memberSanctionClientProvider,
                memberSanctionClient,
                actionRepository,
                publicationStatusChangeService
        );
    }

    @Test
    void executeAction_memberSanctionClientIsNotConfigured_doesNotSaveAction() {
        prepareReport();
        prepareProperty();
        when(memberSanctionClientProvider.getIfAvailable())
                .thenReturn(null);

        assertThatThrownBy(() -> service.executeAction(
                REPORT_ID,
                request(ReportActionCode.REQUEST_MEMBER_SANCTION),
                adminContext
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.MEMBER_API_UNAVAILABLE)
        );

        verifyNoInteractions(actionRepository, publicationStatusChangeService);
    }

    private PropertyReport prepareReport() {
        PropertyReport report = mock(PropertyReport.class);
        when(report.getPropertyId()).thenReturn(PROPERTY_ID);
        when(propertyReportRepository.findByReportIdAndDeletedAtIsNull(REPORT_ID))
                .thenReturn(Optional.of(report));
        return report;
    }

    private void prepareActionSave() {
        when(actionRepository.save(any(PropertyReportAction.class)))
                .thenAnswer(invocation -> {
                    PropertyReportAction action = invocation.getArgument(0);
                    ReflectionTestUtils.setField(action, "actionId", ACTION_ID);
                    return action;
                });
    }

    private Property prepareProperty() {
        Property property = mock(Property.class);
        when(property.getAuthorMemberId()).thenReturn(AUTHOR_MEMBER_ID);
        when(propertyRepository.findById(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        return property;
    }

    private PropertyReportActionRequest request(ReportActionCode actionCode) {
        return new PropertyReportActionRequest(actionCode, REASON);
    }

    private void assertSavedAction(ReportActionCode actionCode) {
        ArgumentCaptor<PropertyReportAction> captor =
                ArgumentCaptor.forClass(PropertyReportAction.class);
        verify(actionRepository).save(captor.capture());

        PropertyReportAction savedAction = captor.getValue();
        assertThat(savedAction.getReportId()).isEqualTo(REPORT_ID);
        assertThat(savedAction.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(savedAction.getActionCode()).isEqualTo(actionCode);
        assertThat(savedAction.getReason()).isEqualTo(REASON);
        assertThat(savedAction.getActorMemberId())
                .isEqualTo(adminContext.memberId());
        assertThat(savedAction.getExecutedAt()).isNotNull();
    }

    private void assertResponse(
            PropertyReportActionResponse response,
            ReportActionCode actionCode
    ) {
        assertThat(response.actionId()).isEqualTo(ACTION_ID);
        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(response.actionCode()).isEqualTo(actionCode);
        assertThat(response.executedAt()).isNotNull();
    }
}
