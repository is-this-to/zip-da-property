package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.RevisionChangeScope;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
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
import com.zipdaproperty.domain.property.request.PropertyTransactionStatusChangeRequest;
import com.zipdaproperty.domain.property.response.PropertyTransactionStatusChangeResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyTransactionStatusChangeServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long PROPERTY_REVISION_ID =
            100L;

    private static final Long AUTHOR_MEMBER_ID =
            1001L;

    private static final Long OTHER_MEMBER_ID =
            2002L;

    private static final Long CURRENT_VERSION =
            0L;

    private static final Long NEXT_VERSION =
            1L;

    private static final String CHANGE_REASON =
            "매수자와 계약 협의가 시작되었습니다.";

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final PropertyStatusHistoryRepository
            propertyStatusHistoryRepository =
            mock(PropertyStatusHistoryRepository.class);

    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();

    private final TransactionStatusPolicy transactionStatusPolicy =
            new TransactionStatusPolicy();

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PropertyTransactionStatusChangeService
            propertyTransactionStatusChangeService =
            new PropertyTransactionStatusChangeService(
                    propertyRepository,
                    propertyRevisionRepository,
                    propertyStatusHistoryRepository,
                    propertyVersionPolicy,
                    transactionStatusPolicy,
                    objectMapper,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "transaction-status-test-owner"
            );

    @Test
    void change_availableToReserved_savesAllHistoryAuditAndPublishesKafkaEvent() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyRevision persistedRevision =
                mock(PropertyRevision.class);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.RESERVED,
                        CURRENT_VERSION
                );

        prepareSuccessfulChange(
                property,
                persistedRevision,
                TransactionStatus.AVAILABLE,
                TransactionStatus.RESERVED
        );

        PropertyTransactionStatusChangeResponse response =
                propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(NEXT_VERSION);

        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.IN_REVIEW);

        assertThat(response.transactionStatus())
                .isEqualTo(TransactionStatus.RESERVED);

        assertThat(response.verificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);

        verify(property)
                .changeTransactionStatus(
                        TransactionStatus.RESERVED,
                        ownerContext
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(
                        PropertyRevision.class
                );

        verify(propertyRevisionRepository)
                .saveAndFlush(
                        revisionCaptor.capture()
                );

        PropertyRevision savedRevision =
                revisionCaptor.getValue();

        assertThat(savedRevision.getPropertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(savedRevision.getPropertyVersion())
                .isEqualTo(NEXT_VERSION);

        assertThat(savedRevision.getChangeType())
                .isEqualTo(RevisionChangeType.UPDATE);

        assertThat(savedRevision.getChangeScope())
                .isEqualTo(RevisionChangeScope.STATUS);

        assertThat(savedRevision.getChangedFieldsJson())
                .isEqualTo("[\"transactionStatus\"]");

        assertThat(savedRevision.getBeforeSnapshotJson())
                .isEqualTo(
                        "{\"transactionStatus\":\"AVAILABLE\"}"
                );

        assertThat(savedRevision.getAfterSnapshotJson())
                .isEqualTo(
                        "{\"transactionStatus\":\"RESERVED\"}"
                );

        assertThat(savedRevision.getChangeReason())
                .isEqualTo(CHANGE_REASON);

        assertThat(savedRevision.getActorMemberId())
                .isEqualTo(AUTHOR_MEMBER_ID);

        assertThat(savedRevision.getOccurredAt())
                .isNotNull();

        ArgumentCaptor<PropertyStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(
                        PropertyStatusHistory.class
                );

        verify(propertyStatusHistoryRepository)
                .save(
                        historyCaptor.capture()
                );

        PropertyStatusHistory savedHistory =
                historyCaptor.getValue();

        assertThat(savedHistory.getPropertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(savedHistory.getPropertyRevisionId())
                .isEqualTo(PROPERTY_REVISION_ID);

        assertThat(savedHistory.getStatusType())
                .isEqualTo(PropertyStatusType.TRANSACTION);

        assertThat(savedHistory.getBeforeStatus())
                .isEqualTo(TransactionStatus.AVAILABLE.name());

        assertThat(savedHistory.getAfterStatus())
                .isEqualTo(TransactionStatus.RESERVED.name());

        assertThat(savedHistory.getReasonCode())
                .isNull();

        assertThat(savedHistory.getReason())
                .isEqualTo(CHANGE_REASON);

        assertThat(savedHistory.getPropertyVersion())
                .isEqualTo(NEXT_VERSION);

        assertThat(savedHistory.getActorMemberId())
                .isEqualTo(AUTHOR_MEMBER_ID);

        assertThat(savedHistory.getActorRole())
                .isEqualTo(ActorRole.USER);

        assertThat(savedHistory.getTraceId())
                .isEqualTo(ownerContext.traceId());

        assertThat(savedHistory.getOccurredAt())
                .isEqualTo(savedRevision.getOccurredAt());

        ArgumentCaptor<Instant> occurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(propertyAuditEventRecorder)
                .recordPropertyAction(
                        eq(PROPERTY_ID),
                        eq(
                                PropertyAuditActionCode
                                        .PROPERTY_TRANSACTION_STATUS_CHANGED
                        ),
                        eq(CHANGE_REASON),
                        isNull(),
                        occurredAtCaptor.capture(),
                        same(ownerContext)
                );

        verify(propertyKafkaEventPublisher)
                .publishAfterCommit(
                        eq(PROPERTY_ID),
                        eq(NEXT_VERSION),
                        eq(PropertyEventType.PROPERTY_UPDATED),
                        argThat(
                                payload ->
                                        PROPERTY_ID.toString().equals(
                                                payload.get("propertyId")
                                        )
                                                && NEXT_VERSION.equals(
                                                payload.get("version")
                                        )
                                                && TransactionStatus
                                                .AVAILABLE
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "beforeTransactionStatus"
                                                        )
                                                )
                                                && TransactionStatus
                                                .RESERVED
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "transactionStatus"
                                                        )
                                                )
                                                && CHANGE_REASON.equals(
                                                payload.get("reason")
                                        )
                        ),
                        eq(occurredAtCaptor.getValue()),
                        same(ownerContext)
                );
    }

    @Test
    void change_reservedToCompleted_publishesPropertyCompletedKafkaEvent() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyRevision persistedRevision =
                mock(PropertyRevision.class);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.COMPLETED,
                        CURRENT_VERSION
                );

        prepareSuccessfulChange(
                property,
                persistedRevision,
                TransactionStatus.RESERVED,
                TransactionStatus.COMPLETED
        );

        PropertyTransactionStatusChangeResponse response =
                propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                );

        assertThat(response.transactionStatus())
                .isEqualTo(TransactionStatus.COMPLETED);

        verify(propertyAuditEventRecorder)
                .recordPropertyAction(
                        eq(PROPERTY_ID),
                        eq(
                                PropertyAuditActionCode
                                        .PROPERTY_TRANSACTION_STATUS_CHANGED
                        ),
                        eq(CHANGE_REASON),
                        isNull(),
                        any(Instant.class),
                        same(ownerContext)
                );

        verify(propertyKafkaEventPublisher)
                .publishAfterCommit(
                        eq(PROPERTY_ID),
                        eq(NEXT_VERSION),
                        eq(PropertyEventType.PROPERTY_COMPLETED),
                        argThat(
                                payload ->
                                        TransactionStatus
                                                .RESERVED
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "beforeTransactionStatus"
                                                        )
                                                )
                                                && TransactionStatus
                                                .COMPLETED
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "transactionStatus"
                                                        )
                                                )
                        ),
                        any(Instant.class),
                        same(ownerContext)
                );
    }

    @Test
    void change_staleVersion_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(NEXT_VERSION);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.RESERVED,
                        CURRENT_VERSION
                );

        assertThatThrownBy(
                () -> propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.VERSION_CONFLICT
                        )
        );

        verify(
                property,
                never()
        ).changeTransactionStatus(
                any(TransactionStatus.class),
                any(ActorContext.class)
        );

        verifyNoPersistenceAfterPropertySave();
        verifyNoDomainEventsRecorded();
    }

    @Test
    void change_differentMember_throwsOwnershipRequired() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.RESERVED,
                        CURRENT_VERSION
                );

        ActorContext otherMemberContext =
                ActorContext.member(
                        OTHER_MEMBER_ID,
                        ActorRole.USER,
                        "transaction-status-test-other-member"
                );

        assertThatThrownBy(
                () -> propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        otherMemberContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .PROPERTY_OWNERSHIP_REQUIRED
                        )
        );

        verify(
                property,
                never()
        ).changeTransactionStatus(
                any(TransactionStatus.class),
                any(ActorContext.class)
        );

        verifyNoPersistenceAfterPropertySave();
        verifyNoDomainEventsRecorded();
    }

    @Test
    void change_availableToCompleted_throwsInvalidStatusTransition() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.COMPLETED,
                        CURRENT_VERSION
                );

        when(property.getTransactionStatus())
                .thenReturn(TransactionStatus.AVAILABLE);

        assertThatThrownBy(
                () -> propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .INVALID_STATUS_TRANSITION
                        )
        );

        verify(
                property,
                never()
        ).changeTransactionStatus(
                any(TransactionStatus.class),
                any(ActorContext.class)
        );

        verifyNoPersistenceAfterPropertySave();
        verifyNoDomainEventsRecorded();
    }

    @Test
    void change_optimisticLockFailure_throwsVersionConflictAndDoesNotRecordEvents() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyTransactionStatusChangeRequest request =
                createRequest(
                        TransactionStatus.RESERVED,
                        CURRENT_VERSION
                );

        when(property.getTransactionStatus())
                .thenReturn(TransactionStatus.AVAILABLE);

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"transactionStatus\":\"AVAILABLE\"}"
                );

        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "거래 상태 동시 변경 충돌 테스트"
                        )
                );

        assertThatThrownBy(
                () -> propertyTransactionStatusChangeService.change(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.VERSION_CONFLICT
                        )
        );

        verify(property)
                .changeTransactionStatus(
                        TransactionStatus.RESERVED,
                        ownerContext
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        verify(
                propertyRevisionRepository,
                never()
        ).saveAndFlush(
                any(PropertyRevision.class)
        );

        verify(
                propertyStatusHistoryRepository,
                never()
        ).save(
                any(PropertyStatusHistory.class)
        );

        verifyNoDomainEventsRecorded();
    }

    private Property prepareExistingProperty(
            Long currentVersion
    ) {
        Property property =
                mock(Property.class);

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(
                Optional.of(property)
        );

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getAuthorMemberId())
                .thenReturn(AUTHOR_MEMBER_ID);

        when(property.getVersion())
                .thenReturn(currentVersion);

        return property;
    }

    private void prepareSuccessfulChange(
            Property property,
            PropertyRevision persistedRevision,
            TransactionStatus beforeStatus,
            TransactionStatus afterStatus
    ) {
        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION
                );

        when(property.getTransactionStatus())
                .thenReturn(
                        beforeStatus,
                        afterStatus,
                        afterStatus,
                        afterStatus,
                        afterStatus
                );

        when(property.getPublicationStatus())
                .thenReturn(PublicationStatus.IN_REVIEW);

        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"transactionStatus\":\""
                                + beforeStatus.name()
                                + "\"}",
                        "{\"transactionStatus\":\""
                                + afterStatus.name()
                                + "\"}"
                );

        when(
                objectMapper.writeValueAsString(
                        List.of("transactionStatus")
                )
        ).thenReturn(
                "[\"transactionStatus\"]"
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        when(persistedRevision.getPropertyRevisionId())
                .thenReturn(PROPERTY_REVISION_ID);

        when(
                propertyRevisionRepository.saveAndFlush(
                        any(PropertyRevision.class)
                )
        ).thenReturn(persistedRevision);
    }

    private void verifyNoPersistenceAfterPropertySave() {
        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).saveAndFlush(
                any(PropertyRevision.class)
        );

        verify(
                propertyStatusHistoryRepository,
                never()
        ).save(
                any(PropertyStatusHistory.class)
        );
    }

    private void verifyNoDomainEventsRecorded() {
        verify(
                propertyAuditEventRecorder,
                never()
        ).recordPropertyAction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        verify(
                propertyKafkaEventPublisher,
                never()
        ).publishAfterCommit(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private PropertyTransactionStatusChangeRequest createRequest(
            TransactionStatus targetStatus,
            Long version
    ) {
        return new PropertyTransactionStatusChangeRequest(
                targetStatus,
                version,
                CHANGE_REASON
        );
    }
}
