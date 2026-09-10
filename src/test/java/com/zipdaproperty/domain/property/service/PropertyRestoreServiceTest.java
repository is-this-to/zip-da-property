package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.RevisionChangeScope;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyRestoreRequest;
import com.zipdaproperty.domain.property.response.PropertyRestoreResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
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

class PropertyRestoreServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long REGION_ID =
            53390L;

    private static final Long ADMIN_MEMBER_ID =
            3003L;

    private static final Long USER_MEMBER_ID =
            1001L;

    private static final Long CURRENT_VERSION =
            3L;

    private static final Long NEXT_VERSION =
            4L;

    private static final String RESTORE_REASON =
            "관리자가 삭제 사유를 확인하고 매물을 복구했습니다.";

    private static final List<String> RESTORE_CHANGED_FIELDS =
            List.of(
                    "deletedAt",
                    "deletedByMemberId",
                    "deletedByRole",
                    "deleteReason"
            );

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final RegionRepository regionRepository =
            mock(RegionRepository.class);

    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PropertyRestoreService propertyRestoreService =
            new PropertyRestoreService(
                    propertyRepository,
                    propertyRevisionRepository,
                    regionRepository,
                    propertyVersionPolicy,
                    objectMapper,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher
            );

    private final ActorContext adminContext =
            ActorContext.member(
                    ADMIN_MEMBER_ID,
                    ActorRole.CS_ADMIN,
                    "property-restore-test-admin"
            );

    @Test
    void restore_adminAndValidReference_savesRevisionAuditAndPublishesKafkaEvent() {
        Property property =
                prepareDeletedProperty(
                        CURRENT_VERSION
                );

        PropertyRestoreRequest request =
                createRequest(CURRENT_VERSION);

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getRegionId())
                .thenReturn(REGION_ID);

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION
                );

        when(property.getPublicationStatus())
                .thenReturn(
                        PublicationStatus.IN_REVIEW
                );

        when(property.getTransactionStatus())
                .thenReturn(
                        TransactionStatus.AVAILABLE
                );

        when(property.getVerificationStatus())
                .thenReturn(
                        VerificationStatus.UNVERIFIED
                );

        when(property.getDeletedAt())
                .thenReturn(null);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(
                                REGION_ID
                        )
        ).thenReturn(
                Optional.of(
                        mock(Region.class)
                )
        );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"deletedAt\":\"deleted\"}",
                        "{\"deletedAt\":null}"
                );

        when(
                objectMapper.writeValueAsString(
                        RESTORE_CHANGED_FIELDS
                )
        ).thenReturn(
                """
                [
                  "deletedAt",
                  "deletedByMemberId",
                  "deletedByRole",
                  "deleteReason"
                ]
                """
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        PropertyRestoreResponse response =
                propertyRestoreService.restore(
                        PROPERTY_ID,
                        request,
                        adminContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(NEXT_VERSION);

        assertThat(response.publicationStatus())
                .isEqualTo(
                        PublicationStatus.IN_REVIEW
                );

        assertThat(response.transactionStatus())
                .isEqualTo(
                        TransactionStatus.AVAILABLE
                );

        assertThat(response.verificationStatus())
                .isEqualTo(
                        VerificationStatus.UNVERIFIED
                );

        verify(regionRepository)
                .findByRegionIdAndIsActiveTrue(
                        REGION_ID
                );

        verify(property)
                .restore(adminContext);

        verify(propertyRepository)
                .saveAndFlush(property);

        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(
                        PropertyRevision.class
                );

        verify(propertyRevisionRepository)
                .save(revisionCaptor.capture());

        PropertyRevision savedRevision =
                revisionCaptor.getValue();

        assertThat(savedRevision.getPropertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(savedRevision.getPropertyVersion())
                .isEqualTo(NEXT_VERSION);

        assertThat(savedRevision.getChangeType())
                .isEqualTo(
                        RevisionChangeType.RESTORE
                );

        assertThat(savedRevision.getChangeScope())
                .isEqualTo(
                        RevisionChangeScope.PROPERTY
                );

        assertThat(savedRevision.getBeforeSnapshotJson())
                .isEqualTo(
                        "{\"deletedAt\":\"deleted\"}"
                );

        assertThat(savedRevision.getAfterSnapshotJson())
                .isEqualTo(
                        "{\"deletedAt\":null}"
                );

        assertThat(savedRevision.getChangeReason())
                .isEqualTo(RESTORE_REASON);

        assertThat(savedRevision.getActorMemberId())
                .isEqualTo(ADMIN_MEMBER_ID);

        assertThat(savedRevision.getActorRole())
                .isEqualTo(ActorRole.CS_ADMIN);

        assertThat(savedRevision.getOccurredAt())
                .isNotNull();

        ArgumentCaptor<Instant> occurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(propertyAuditEventRecorder)
                .recordPropertyAction(
                        eq(PROPERTY_ID),
                        eq(
                                PropertyAuditActionCode
                                        .PROPERTY_RESTORED
                        ),
                        eq(RESTORE_REASON),
                        isNull(),
                        occurredAtCaptor.capture(),
                        same(adminContext)
                );

        assertThat(occurredAtCaptor.getValue())
                .isEqualTo(savedRevision.getOccurredAt());

        verify(propertyKafkaEventPublisher)
                .publishAfterCommit(
                        eq(PROPERTY_ID),
                        eq(NEXT_VERSION),
                        eq(
                                PropertyEventType
                                        .PROPERTY_REACTIVATED
                        ),
                        argThat(
                                payload ->
                                        PROPERTY_ID.toString().equals(
                                                payload.get("propertyId")
                                        )
                                                && NEXT_VERSION.equals(
                                                payload.get("version")
                                        )
                                                && payload.containsKey(
                                                "deletedAt"
                                        )
                                                && payload.get(
                                                "deletedAt"
                                        ) == null
                                                && ADMIN_MEMBER_ID
                                                .toString()
                                                .equals(
                                                        payload.get(
                                                                "restoredByMemberId"
                                                        )
                                                )
                                                && ActorRole.CS_ADMIN
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "restoredByRole"
                                                        )
                                                )
                                                && RESTORE_REASON.equals(
                                                payload.get(
                                                        "restoreReason"
                                                )
                                        )
                                                && PublicationStatus
                                                .IN_REVIEW
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "publicationStatus"
                                                        )
                                                )
                                                && TransactionStatus
                                                .AVAILABLE
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "transactionStatus"
                                                        )
                                                )
                        ),
                        eq(occurredAtCaptor.getValue()),
                        same(adminContext)
                );
    }

    @Test
    void restore_generalUser_throwsForbiddenAndDoesNotRecordEvents() {
        prepareDeletedProperty(
                CURRENT_VERSION
        );

        PropertyRestoreRequest request =
                createRequest(CURRENT_VERSION);

        ActorContext userContext =
                ActorContext.member(
                        USER_MEMBER_ID,
                        ActorRole.USER,
                        "property-restore-test-user"
                );

        assertThatThrownBy(
                () -> propertyRestoreService.restore(
                        PROPERTY_ID,
                        request,
                        userContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.FORBIDDEN
                        )
        );

        verify(
                regionRepository,
                never()
        ).findByRegionIdAndIsActiveTrue(
                any(Long.class)
        );

        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );

        verifyNoDomainEventsRecorded();
    }

    @Test
    void restore_staleVersion_throwsVersionConflictAndDoesNotRecordEvents() {
        Property property =
                prepareDeletedProperty(
                        NEXT_VERSION
                );

        PropertyRestoreRequest request =
                createRequest(CURRENT_VERSION);

        assertThatThrownBy(
                () -> propertyRestoreService.restore(
                        PROPERTY_ID,
                        request,
                        adminContext
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
        ).restore(
                any(ActorContext.class)
        );

        verify(
                regionRepository,
                never()
        ).findByRegionIdAndIsActiveTrue(
                any(Long.class)
        );

        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );

        verifyNoDomainEventsRecorded();
    }

    @Test
    void restore_inactiveRegion_throwsReferenceInvalidAndDoesNotRecordEvents() {
        Property property =
                prepareDeletedProperty(
                        CURRENT_VERSION
                );

        PropertyRestoreRequest request =
                createRequest(CURRENT_VERSION);

        when(property.getRegionId())
                .thenReturn(REGION_ID);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(
                                REGION_ID
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> propertyRestoreService.restore(
                        PROPERTY_ID,
                        request,
                        adminContext
                )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode
                                        .RESTORE_REFERENCE_INVALID
                        )
        );

        verify(
                property,
                never()
        ).restore(
                any(ActorContext.class)
        );

        verify(
                propertyRepository,
                never()
        ).saveAndFlush(
                any(Property.class)
        );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );

        verifyNoDomainEventsRecorded();
    }

    @Test
    void restore_optimisticLockFailure_throwsVersionConflictAndDoesNotRecordEvents() {
        Property property =
                prepareDeletedProperty(
                        CURRENT_VERSION
                );

        PropertyRestoreRequest request =
                createRequest(CURRENT_VERSION);

        when(property.getRegionId())
                .thenReturn(REGION_ID);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(
                                REGION_ID
                        )
        ).thenReturn(
                Optional.of(
                        mock(Region.class)
                )
        );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"deletedAt\":\"deleted\"}"
                );

        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "매물 복구 동시 변경 충돌 테스트"
                        )
                );

        assertThatThrownBy(
                () -> propertyRestoreService.restore(
                        PROPERTY_ID,
                        request,
                        adminContext
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
                .restore(
                        eq(adminContext)
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );

        verifyNoDomainEventsRecorded();
    }

    private Property prepareDeletedProperty(
            Long currentVersion
    ) {
        Property property =
                mock(Property.class);

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNotNull(
                                PROPERTY_ID
                        )
        ).thenReturn(
                Optional.of(property)
        );

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getVersion())
                .thenReturn(currentVersion);

        return property;
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

    private PropertyRestoreRequest createRequest(
            Long version
    ) {
        return new PropertyRestoreRequest(
                version,
                RESTORE_REASON
        );
    }
}
