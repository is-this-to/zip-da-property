package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.request.PropertyPublicationStatusChangeRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicationStatusChangeResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyPublicationStatusChangeServiceTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final Long REVISION_ID = 501L;
    private static final Long OWNER_ID = 1001L;
    private static final Long OTHER_ID = 2002L;
    private static final Long CURRENT_VERSION = 0L;
    private static final Long NEXT_VERSION = 1L;
    private static final String REASON = "공개 상태 변경 테스트";

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyRevisionRepository propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);
    private final PropertyStatusHistoryRepository propertyStatusHistoryRepository =
            mock(PropertyStatusHistoryRepository.class);
    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();
    private final PublicationStatusPolicy publicationStatusPolicy =
            new PublicationStatusPolicy();
    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);
    private final PropertyAuditEventRecorder propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyKafkaEventPublisher propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PropertyPublicationStatusChangeService service =
            new PropertyPublicationStatusChangeService(
                    propertyRepository,
                    propertyRevisionRepository,
                    propertyStatusHistoryRepository,
                    propertyVersionPolicy,
                    publicationStatusPolicy,
                    objectMapper,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    OWNER_ID,
                    ActorRole.USER,
                    "publication-owner"
            );

    private final ActorContext adminContext =
            ActorContext.member(
                    OTHER_ID,
                    ActorRole.CS_ADMIN,
                    "publication-admin"
            );

    @Test
    void change_adminApproves_savesRevisionHistoryAuditAndKafkaEvent() {
        Property property = prepareSuccessfulChange(
                PublicationStatus.IN_REVIEW,
                PublicationStatus.PUBLISHED,
                TransactionStatus.AVAILABLE
        );

        PropertyPublicationStatusChangeResponse response =
                service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.PUBLISHED),
                        adminContext
                );

        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(response.version()).isEqualTo(NEXT_VERSION);
        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.PUBLISHED);

        verify(property).changePublicationStatus(
                eq(PublicationStatus.PUBLISHED),
                eq(adminContext),
                any()
        );

        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(PropertyRevision.class);
        verify(propertyRevisionRepository)
                .saveAndFlush(revisionCaptor.capture());

        assertThat(revisionCaptor.getValue().getChangeScope().name())
                .isEqualTo("STATUS");
        assertThat(revisionCaptor.getValue().getChangedFieldsJson())
                .isEqualTo("[\"publicationStatus\"]");

        ArgumentCaptor<PropertyStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(PropertyStatusHistory.class);
        verify(propertyStatusHistoryRepository)
                .save(historyCaptor.capture());

        assertThat(historyCaptor.getValue().getStatusType())
                .isEqualTo(PropertyStatusType.PUBLICATION);
        assertThat(historyCaptor.getValue().getBeforeStatus())
                .isEqualTo(PublicationStatus.IN_REVIEW.name());
        assertThat(historyCaptor.getValue().getAfterStatus())
                .isEqualTo(PublicationStatus.PUBLISHED.name());

        verify(propertyAuditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID),
                eq(PropertyAuditActionCode.PROPERTY_PUBLICATION_STATUS_CHANGED),
                eq(REASON),
                eq(null),
                any(),
                eq(adminContext)
        );

        verify(propertyKafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(NEXT_VERSION),
                eq(PropertyEventType.PROPERTY_PUBLISHED),
                any(),
                any(),
                eq(adminContext)
        );
    }

    @Test
    void change_ownerHidesPublishedProperty_succeeds() {
        prepareSuccessfulChange(
                PublicationStatus.PUBLISHED,
                PublicationStatus.HIDDEN,
                TransactionStatus.RESERVED
        );

        PropertyPublicationStatusChangeResponse response =
                service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.HIDDEN),
                        ownerContext
                );

        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.HIDDEN);

        verify(propertyKafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(NEXT_VERSION),
                eq(PropertyEventType.PROPERTY_HIDDEN),
                any(),
                any(),
                eq(ownerContext)
        );
    }

    @Test
    void change_ownerResubmitsRejectedProperty_succeeds() {
        prepareSuccessfulChange(
                PublicationStatus.REJECTED,
                PublicationStatus.IN_REVIEW,
                TransactionStatus.AVAILABLE
        );

        PropertyPublicationStatusChangeResponse response =
                service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.IN_REVIEW),
                        ownerContext
                );

        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.IN_REVIEW);

        verify(propertyKafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(NEXT_VERSION),
                eq(PropertyEventType.PROPERTY_RESUBMITTED),
                any(),
                any(),
                eq(ownerContext)
        );
    }

    @Test
    void change_ownerCannotApproveReview() {
        Property property = prepareProperty(
                PublicationStatus.IN_REVIEW,
                TransactionStatus.AVAILABLE
        );

        assertPermissionDenied(
                () -> service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.PUBLISHED),
                        ownerContext
                )
        );

        verify(property, never()).changePublicationStatus(
                any(),
                any(),
                any()
        );
        verifyNoPersistenceOrEvents();
    }

    @Test
    void change_adminCannotResubmitRejectedProperty() {
        Property property = prepareProperty(
                PublicationStatus.REJECTED,
                TransactionStatus.AVAILABLE
        );

        assertPermissionDenied(
                () -> service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.IN_REVIEW),
                        adminContext
                )
        );

        verify(property, never()).changePublicationStatus(
                any(),
                any(),
                any()
        );
        verifyNoPersistenceOrEvents();
    }

    @Test
    void change_reservedHiddenPropertyCannotBeRepublished() {
        Property property = prepareProperty(
                PublicationStatus.HIDDEN,
                TransactionStatus.RESERVED
        );

        assertThatThrownBy(
                () -> service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.PUBLISHED),
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getCustomResponseCode()
                ).isEqualTo(
                        CustomResponseCode.INVALID_STATUS_TRANSITION
                )
        );

        verify(property, never()).changePublicationStatus(
                any(),
                any(),
                any()
        );
        verifyNoPersistenceOrEvents();
    }

    @Test
    void change_versionMismatch_doesNotModifyProperty() {
        Property property = prepareProperty(
                PublicationStatus.PUBLISHED,
                TransactionStatus.AVAILABLE
        );

        PropertyPublicationStatusChangeRequest request =
                new PropertyPublicationStatusChangeRequest(
                        PublicationStatus.HIDDEN,
                        99L,
                        REASON
                );

        assertThatThrownBy(
                () -> service.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getCustomResponseCode()
                ).isEqualTo(CustomResponseCode.VERSION_CONFLICT)
        );

        verify(property, never()).changePublicationStatus(
                any(),
                any(),
                any()
        );
        verifyNoPersistenceOrEvents();
    }

    @Test
    void change_optimisticLockFailure_recordsNoHistoryOrEvents() {
        Property property = prepareProperty(
                PublicationStatus.PUBLISHED,
                TransactionStatus.AVAILABLE
        );

        when(objectMapper.writeValueAsString(property))
                .thenReturn("{\"publicationStatus\":\"PUBLISHED\"}");
        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "공개 상태 동시 변경 충돌"
                        )
                );

        assertThatThrownBy(
                () -> service.change(
                        PROPERTY_ID,
                        request(PublicationStatus.HIDDEN),
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(
                        exception.getCustomResponseCode()
                ).isEqualTo(CustomResponseCode.VERSION_CONFLICT)
        );

        verify(property).changePublicationStatus(
                eq(PublicationStatus.HIDDEN),
                eq(ownerContext),
                any()
        );
        verifyNoHistoryOrEvents();
    }

    private Property prepareProperty(
            PublicationStatus publicationStatus,
            TransactionStatus transactionStatus
    ) {
        Property property = mock(Property.class);

        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getAuthorMemberId()).thenReturn(OWNER_ID);
        when(property.getVersion()).thenReturn(CURRENT_VERSION);
        when(property.getPublicationStatus()).thenReturn(publicationStatus);
        when(property.getTransactionStatus()).thenReturn(transactionStatus);

        return property;
    }

    private Property prepareSuccessfulChange(
            PublicationStatus beforeStatus,
            PublicationStatus afterStatus,
            TransactionStatus transactionStatus
    ) {
        Property property = prepareProperty(beforeStatus, transactionStatus);
        PropertyRevision persistedRevision = mock(PropertyRevision.class);

        when(property.getVersion()).thenReturn(
                CURRENT_VERSION,
                NEXT_VERSION,
                NEXT_VERSION,
                NEXT_VERSION,
                NEXT_VERSION,
                NEXT_VERSION
        );
        when(property.getPublicationStatus()).thenReturn(
                beforeStatus,
                afterStatus,
                afterStatus,
                afterStatus,
                afterStatus,
                afterStatus
        );
        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);
        when(objectMapper.writeValueAsString(property)).thenReturn(
                "{\"publicationStatus\":\"" + beforeStatus + "\"}",
                "{\"publicationStatus\":\"" + afterStatus + "\"}"
        );
        when(objectMapper.writeValueAsString(List.of("publicationStatus")))
                .thenReturn("[\"publicationStatus\"]");
        when(propertyRepository.saveAndFlush(property)).thenReturn(property);
        when(persistedRevision.getPropertyRevisionId())
                .thenReturn(REVISION_ID);
        when(propertyRevisionRepository.saveAndFlush(any()))
                .thenReturn(persistedRevision);

        return property;
    }

    private PropertyPublicationStatusChangeRequest request(
            PublicationStatus targetStatus
    ) {
        return new PropertyPublicationStatusChangeRequest(
                targetStatus,
                CURRENT_VERSION,
                REASON
        );
    }

    private void assertPermissionDenied(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED
                        )
                );
    }

    private void verifyNoPersistenceOrEvents() {
        verify(propertyRepository, never()).saveAndFlush(any());
        verifyNoHistoryOrEvents();
    }

    private void verifyNoHistoryOrEvents() {
        verify(propertyRevisionRepository, never()).saveAndFlush(any());
        verify(propertyStatusHistoryRepository, never()).save(any());
        verify(propertyAuditEventRecorder, never())
                .recordPropertyAction(any(), any(), any(), any(), any(), any());
        verify(propertyKafkaEventPublisher, never())
                .publishAfterCommit(any(), any(), any(), any(), any(), any());
    }
}
