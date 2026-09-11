package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageSyncService;
import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.option.service.PropertyOptionCommandService;
import com.zipdaproperty.domain.option.service.PropertyOptionCommandService.OptionSyncPlan;
import com.zipdaproperty.domain.property.request.PropertyOptionRequest;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.command.PropertyAddressCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.RevisionChangeType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.member.service.MemberWritePermissionService;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.domain.property.request.PropertyAddressRequest;
import com.zipdaproperty.domain.property.response.PropertyUpdateResponse;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyUpdateServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long REGION_ID = 53390L;

    private static final Long AUTHOR_MEMBER_ID = 1001L;

    private static final Long OTHER_MEMBER_ID = 2002L;

    private static final Long CURRENT_VERSION = 0L;

    private static final Long NEXT_VERSION = 1L;

    private static final String BEFORE_TITLE =
            "수정 전 제목";

    private static final String AFTER_TITLE =
            "수정 후 제목";

    private static final String UPDATE_REASON =
            "매물 핵심 정보가 수정되었습니다.";

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyRevisionRepository
            propertyRevisionRepository =
            mock(PropertyRevisionRepository.class);

    private final RegionRepository regionRepository =
            mock(RegionRepository.class);

    private final PropertyVersionPolicy propertyVersionPolicy =
            new PropertyVersionPolicy();

    private final PropertyUpdateCommandFactory
            propertyUpdateCommandFactory =
            mock(PropertyUpdateCommandFactory.class);

    private final PropertyUpdatePolicy propertyUpdatePolicy =
            mock(PropertyUpdatePolicy.class);

    private final PropertyPricePolicy propertyPricePolicy =
            mock(PropertyPricePolicy.class);

    private final PropertyImageSyncService propertyImageSyncService =
            mock(PropertyImageSyncService.class);

    private final EntityManager entityManager =
            mock(EntityManager.class);

    private final ObjectMapper objectMapper =
            mock(ObjectMapper.class);

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder =
            mock(PropertyAuditEventRecorder.class);

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);

    private final PropertyAddressService propertyAddressService =
            mock(PropertyAddressService.class);

    private final MemberWritePermissionService
            memberWritePermissionService =
            mock(MemberWritePermissionService.class);

    private final PropertyOptionCommandService propertyOptionCommandService =
            mock(PropertyOptionCommandService.class);

    private final PropertyUpdateService propertyUpdateService =
            new PropertyUpdateService(
                    propertyRepository,
                    propertyRevisionRepository,
                    regionRepository,
                    propertyVersionPolicy,
                    propertyUpdateCommandFactory,
                    propertyUpdatePolicy,
                    propertyPricePolicy,
                    propertyImageSyncService,
                    entityManager,
                    objectMapper,
                    propertyAuditEventRecorder,
                    propertyKafkaEventPublisher,
                    propertyAddressService,
                    propertyOptionCommandService,
                    memberWritePermissionService
            );

    private final ActorContext ownerContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-update-test-owner"
            );

    @Test
    void update_addressOnlyForcesVersionIncrementAndChangesAddress() {
        Property property = prepareExistingProperty(CURRENT_VERSION);
        prepareCompletePropertyState(property);

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION
                );

        PropertyAddressRequest addressRequest =
                new PropertyAddressRequest(
                        "대구 수성구 달구벌대로 2450",
                        "대구광역시 수성구 범어동 123",
                        "2726010100",
                        new BigDecimal("128.625123"),
                        new BigDecimal("35.859321")
                );

        PropertyUpdateRequest request =
                new PropertyUpdateRequest(
                        CURRENT_VERSION,
                        Map.of(),
                        addressRequest
                );

        PreparedPropertyAddress preparedAddress =
                mock(PreparedPropertyAddress.class);

        when(preparedAddress.regionId())
                .thenReturn(REGION_ID);

        when(
                propertyAddressService.prepare(
                        eq(PROPERTY_ID),
                        any(PropertyAddressCommand.class)
                )
        ).thenReturn(preparedAddress);

        PropertyUpdateCommand command =
                createUpdateCommand(
                        CURRENT_VERSION,
                        BEFORE_TITLE
                );

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        REGION_ID
                )
        ).thenReturn(command);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(Optional.of(mock(Region.class)));

        when(objectMapper.writeValueAsString(property))
                .thenReturn("{}", "{}");

        when(
                objectMapper.writeValueAsString(
                        List.of("address")
                )
        ).thenReturn("[\"address\"]");

        PropertyUpdateResponse response =
                propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        ownerContext
                );

        assertThat(response.version()).isEqualTo(NEXT_VERSION);

        verify(entityManager).lock(
                property,
                LockModeType.PESSIMISTIC_FORCE_INCREMENT
        );
        verify(entityManager).flush();

        verify(propertyRepository, never())
                .saveAndFlush(any(Property.class));

        verify(propertyAddressService).change(
                property,
                preparedAddress,
                ownerContext
        );
    }

    @Test
    void update_ownerAndValidVersion_savesPropertyRevisionAuditAndPublishesKafkaEvent() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        prepareCompletePropertyState(property);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        PropertyUpdateCommand command =
                createUpdateCommand(
                        CURRENT_VERSION,
                        AFTER_TITLE
                );

        when(property.getVersion())
                .thenReturn(
                        CURRENT_VERSION,
                        NEXT_VERSION,
                        NEXT_VERSION
                );

        when(property.getTitle())
                .thenReturn(
                        BEFORE_TITLE,
                        AFTER_TITLE
                );

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        null
                )
        ).thenReturn(command);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"title\":\"수정 전 제목\"}",
                        "{\"title\":\"수정 후 제목\"}"
                );

        when(
                objectMapper.writeValueAsString(
                        List.of("title")
                )
        ).thenReturn("[\"title\"]");

        PropertyUpdateResponse response =
                propertyUpdateService.update(
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
                .isEqualTo(TransactionStatus.AVAILABLE);

        assertThat(response.verificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);

        verify(propertyUpdatePolicy)
                .validate(command);

        verify(propertyPricePolicy)
                .validate(
                        TransactionType.SALE,
                        500_000_000L,
                        null,
                        null
                );

        verify(property)
                .update(
                        command,
                        ownerContext
                );

        verify(propertyRepository)
                .saveAndFlush(property);

        verifyNoInteractions(
                propertyImageSyncService,
                entityManager
        );

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
                .isEqualTo(RevisionChangeType.UPDATE);

        assertThat(savedRevision.getChangedFieldsJson())
                .isEqualTo("[\"title\"]");

        assertThat(savedRevision.getActorMemberId())
                .isEqualTo(AUTHOR_MEMBER_ID);

        ArgumentCaptor<Instant> occurredAtCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(propertyAuditEventRecorder)
                .recordPropertyAction(
                        eq(PROPERTY_ID),
                        eq(PropertyAuditActionCode.PROPERTY_UPDATED),
                        eq(UPDATE_REASON),
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
                                                && List.of("title").equals(
                                                payload.get("changedFields")
                                        )
                                                && REGION_ID.toString().equals(
                                                payload.get("regionId")
                                        )
                                                && AUTHOR_MEMBER_ID
                                                .toString()
                                                .equals(
                                                        payload.get(
                                                                "authorMemberId"
                                                        )
                                                )
                                                && PublisherType
                                                .DIRECT_OWNER
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "publisherType"
                                                        )
                                                )
                                                && PropertyType
                                                .APARTMENT
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "propertyType"
                                                        )
                                                )
                                                && TransactionType
                                                .SALE
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "transactionType"
                                                        )
                                                )
                                                && AFTER_TITLE.equals(
                                                payload.get("title")
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
                                                && VerificationStatus
                                                .UNVERIFIED
                                                .name()
                                                .equals(
                                                        payload.get(
                                                                "verificationStatus"
                                                        )
                                                )
                        ),
                        eq(occurredAtCaptor.getValue()),
                        same(ownerContext)
                );

        verify(memberWritePermissionService)
                .validate(
                        AUTHOR_MEMBER_ID,
                        ActorRole.USER,
                        MemberPermissionAction.PROPERTY_UPDATE
                );
    }

    @Test
    void update_fileIdsOnly_updatesImagesVersionRevisionAndAudit() {
        Property property = prepareExistingProperty(CURRENT_VERSION);
        prepareCompletePropertyState(property);
        when(property.getTitle()).thenReturn(BEFORE_TITLE);

        List<Long> fileIds = List.of(2L, 3L);
        JsonNode fileIdsNode = mock(JsonNode.class);
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                CURRENT_VERSION,
                Map.of(),
                fileIds
        );
        PropertyUpdateCommand command = createUpdateCommand(
                CURRENT_VERSION,
                BEFORE_TITLE
        );
        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        null
                )
        ).thenReturn(command);
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(mock(Region.class)));
        when(propertyImageSyncService.prepareSync(PROPERTY_ID, fileIds))
                .thenReturn(new PropertyImageSyncService.SyncPlan(
                        List.of(1L, 2L),
                        true
                ));
        prepareImageSnapshots(property, fileIdsNode);
        when(objectMapper.writeValueAsString(List.of("fileIds")))
                .thenReturn("[\"fileIds\"]");
        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        PropertyUpdateResponse response = propertyUpdateService.update(
                PROPERTY_ID,
                request,
                ownerContext
        );

        assertThat(response.version()).isEqualTo(NEXT_VERSION);
        verify(property, never()).update(any(), any());
        verify(entityManager).lock(
                property,
                LockModeType.OPTIMISTIC_FORCE_INCREMENT
        );
        verify(propertyImageSyncService).syncImages(
                PROPERTY_ID,
                fileIds,
                ownerContext
        );

        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(PropertyRevision.class);
        verify(propertyRevisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getPropertyVersion())
                .isEqualTo(NEXT_VERSION);
        assertThat(revisionCaptor.getValue().getChangedFieldsJson())
                .isEqualTo("[\"fileIds\"]");
        verify(propertyAuditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID),
                eq(PropertyAuditActionCode.PROPERTY_UPDATED),
                eq(UPDATE_REASON),
                isNull(),
                any(Instant.class),
                same(ownerContext)
        );
    }

    @Test
    void update_propertyFieldAndFileIds_updatesBothWithoutForcedIncrement() {
        Property property = prepareExistingProperty(CURRENT_VERSION);
        prepareCompletePropertyState(property);
        when(property.getTitle()).thenReturn(BEFORE_TITLE, AFTER_TITLE);
        when(property.getVersion()).thenReturn(
                CURRENT_VERSION,
                NEXT_VERSION,
                NEXT_VERSION
        );

        List<Long> fileIds = List.of(3L, 1L, 4L);
        JsonNode titleNode = mock(JsonNode.class);
        JsonNode fileIdsNode = mock(JsonNode.class);
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                CURRENT_VERSION,
                Map.of("title", titleNode),
                fileIds
        );
        PropertyUpdateCommand command = createUpdateCommand(
                CURRENT_VERSION,
                AFTER_TITLE
        );
        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        null
                )
        ).thenReturn(command);
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(mock(Region.class)));
        when(propertyImageSyncService.prepareSync(PROPERTY_ID, fileIds))
                .thenReturn(new PropertyImageSyncService.SyncPlan(
                        List.of(1L, 2L, 3L),
                        true
                ));
        prepareImageSnapshots(property, fileIdsNode);
        when(objectMapper.writeValueAsString(
                List.of("fileIds", "title")
        )).thenReturn("[\"fileIds\",\"title\"]");
        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);

        propertyUpdateService.update(
                PROPERTY_ID,
                request,
                ownerContext
        );

        verify(property).update(command, ownerContext);
        verifyNoInteractions(entityManager);
        verify(propertyImageSyncService).syncImages(
                PROPERTY_ID,
                fileIds,
                ownerContext
        );
        ArgumentCaptor<PropertyRevision> revisionCaptor =
                ArgumentCaptor.forClass(PropertyRevision.class);
        verify(propertyRevisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getPropertyVersion())
                .isEqualTo(NEXT_VERSION);
        assertThat(revisionCaptor.getValue().getChangedFieldsJson())
                .isEqualTo("[\"fileIds\",\"title\"]");
    }

    @Test
    void update_sameFileIdsOnly_rejectsAsNoActualChanges() {
        Property property = prepareExistingProperty(CURRENT_VERSION);
        prepareCompletePropertyState(property);
        when(property.getTitle()).thenReturn(BEFORE_TITLE);

        List<Long> fileIds = List.of(1L, 2L);

        PropertyUpdateRequest request = new PropertyUpdateRequest(
                CURRENT_VERSION,
                Map.of(),
                fileIds
        );

        PropertyUpdateCommand command = createUpdateCommand(
                CURRENT_VERSION,
                BEFORE_TITLE
        );

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        null
                )
        ).thenReturn(command);

        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID))
                .thenReturn(Optional.of(mock(Region.class)));

        when(propertyImageSyncService.prepareSync(PROPERTY_ID, fileIds))
                .thenReturn(new PropertyImageSyncService.SyncPlan(
                        fileIds,
                        false
                ));

        assertThatThrownBy(() -> propertyUpdateService.update(
                PROPERTY_ID,
                request,
                ownerContext
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );

        verify(propertyImageSyncService, never()).syncImages(
                any(),
                any(),
                any()
        );

        verifyNoInteractions(entityManager);
        verify(propertyRepository, never()).saveAndFlush(any());
        verify(propertyRevisionRepository, never()).save(any());
        verifyNoDomainEventsRecorded();
    }

    @Test
    void update_staleVersionWithFileIds_rejectsBeforeImageComparison() {
        Property property = prepareExistingProperty(NEXT_VERSION);
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                CURRENT_VERSION,
                Map.of(),
                List.of(2L, 1L)
        );

        assertThatThrownBy(() -> propertyUpdateService.update(
                PROPERTY_ID,
                request,
                ownerContext
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.VERSION_CONFLICT)
        );

        verify(propertyUpdateCommandFactory, never()).create(
                any(Property.class),
                any(PropertyUpdateRequest.class),
                isNull()
        );
        verifyNoInteractions(propertyImageSyncService, entityManager);
        verify(propertyRepository, never()).saveAndFlush(any());
        verify(propertyRevisionRepository, never()).save(any());
        verifyNoDomainEventsRecorded();
    }

    @Test
    void update_staleVersion_throwsVersionConflict() {
        Property property =
                prepareExistingProperty(NEXT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
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
                propertyUpdateCommandFactory,
                never()
        ).create(
                any(Property.class),
                any(PropertyUpdateRequest.class),
                isNull()
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

        verifyNoInteractions(
                propertyImageSyncService,
                entityManager
        );

        verifyNoDomainEventsRecorded();
    }

    @Test
    void update_differentMember_throwsOwnershipRequired() {
        prepareExistingProperty(NEXT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        NEXT_VERSION
                );

        ActorContext otherMemberContext =
                ActorContext.member(
                        OTHER_MEMBER_ID,
                        ActorRole.USER,
                        "property-update-test-other-member"
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
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
                propertyUpdateCommandFactory,
                never()
        ).create(
                any(Property.class),
                any(PropertyUpdateRequest.class),
                isNull()
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
    void update_memberPermissionDenied_doesNotChangeProperty() {
        Property property = prepareExistingProperty(CURRENT_VERSION);

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID)
        ).thenReturn(Optional.of(property));

        doThrow(new BusinessException(
                CustomResponseCode.MEMBER_PERMISSION_DENIED,
                "권한 없음"
        )).when(memberWritePermissionService)
                .validate(
                        AUTHOR_MEMBER_ID,
                        ActorRole.USER,
                        MemberPermissionAction.PROPERTY_UPDATE
                );

        PropertyUpdateRequest request = new PropertyUpdateRequest(
                CURRENT_VERSION,
                Map.of("title", mock(JsonNode.class)),
                null
        );

        assertThatThrownBy(() ->
                propertyUpdateService.update(
                        PROPERTY_ID,
                        request,
                        ownerContext
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception)
                                .getCustomResponseCode()
                )
                .isEqualTo(
                        CustomResponseCode.MEMBER_PERMISSION_DENIED
                );

        verify(property, never()).update(any(), any());
        verify(propertyRevisionRepository, never())
                .save(any(PropertyRevision.class));
    }

    @Test
    void update_optimisticLockFailure_throwsVersionConflictAndDoesNotRecordEvents() {
        Property property =
                prepareExistingProperty(CURRENT_VERSION);

        PropertyUpdateRequest request =
                createTitleUpdateRequest(
                        CURRENT_VERSION
                );

        PropertyUpdateCommand command =
                createUpdateCommand(
                        CURRENT_VERSION,
                        AFTER_TITLE
                );

        when(property.getTitle())
                .thenReturn(BEFORE_TITLE);

        when(
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        null
                )
        ).thenReturn(command);

        when(
                regionRepository
                        .findByRegionIdAndIsActiveTrue(REGION_ID)
        ).thenReturn(
                Optional.of(mock(Region.class))
        );

        when(objectMapper.writeValueAsString(property))
                .thenReturn(
                        "{\"title\":\"수정 전 제목\"}"
                );

        when(propertyRepository.saveAndFlush(property))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "동시 수정 충돌 테스트"
                        )
                );

        assertThatThrownBy(
                () -> propertyUpdateService.update(
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
                .update(
                        command,
                        ownerContext
                );

        verify(
                propertyRevisionRepository,
                never()
        ).save(
                any(PropertyRevision.class)
        );

        verifyNoDomainEventsRecorded();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void update_optionsOnly_synchronizesAfterSavedRevisionAndIncrementsVersion(boolean empty) {
        List<PropertyOptionRequest> options = empty ? List.of()
                : List.of(new PropertyOptionRequest("PARKING", "2"));
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION, null, null, null, options);
        Property property = prepareOptionUpdate(request);
        List<PropertyOptionCreateCommand> commands = options.stream().map(PropertyOptionRequest::toCommand).toList();
        when(propertyOptionCommandService.prepareSync(PROPERTY_ID, PropertyType.APARTMENT, commands))
                .thenReturn(new OptionSyncPlan(true));

        PropertyUpdateResponse response = propertyUpdateService.update(PROPERTY_ID, request, ownerContext);

        assertThat(response.version()).isEqualTo(NEXT_VERSION);
        verify(entityManager).lock(property, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        verify(entityManager, never()).lock(property, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        verify(property, never()).update(any(), any());
        verify(objectMapper).writeValueAsString(List.of("options"));
        InOrder order = inOrder(propertyOptionCommandService, propertyRevisionRepository, propertyKafkaEventPublisher);
        order.verify(propertyOptionCommandService).prepareSync(PROPERTY_ID, PropertyType.APARTMENT, commands);
        ArgumentCaptor<PropertyRevision> revision = ArgumentCaptor.forClass(PropertyRevision.class);
        order.verify(propertyRevisionRepository).save(revision.capture());
        assertThat(revision.getValue().getPropertyVersion()).isEqualTo(NEXT_VERSION);
        order.verify(propertyOptionCommandService).synchronizeOptions(
                PROPERTY_ID, 999L, PropertyType.APARTMENT, commands, "options", ownerContext);
        order.verify(propertyKafkaEventPublisher).publishAfterCommit(eq(PROPERTY_ID), eq(NEXT_VERSION),
                eq(PropertyEventType.PROPERTY_UPDATED), argThat(payload ->
                        List.of("options").equals(payload.get("changedFields"))), any(), eq(ownerContext));
        verifyNoInteractions(propertyImageSyncService, propertyAddressService);
    }

    @Test
    void update_optionsNoOpWithoutOtherChanges_rejectsBeforeMutation() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION, null, null, null, List.of());
        Property property = prepareOptionUpdate(request);
        when(propertyOptionCommandService.prepareSync(PROPERTY_ID, PropertyType.APARTMENT, List.of()))
                .thenReturn(new OptionSyncPlan(false));

        assertThatThrownBy(() -> propertyUpdateService.update(PROPERTY_ID, request, ownerContext))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.INVALID_REQUEST);
                    assertThat(exception.getMessage()).contains("실제로 변경된 필드가 없습니다.");
                });
        verify(propertyOptionCommandService, never()).synchronizeOptions(any(), any(), any(), any(), any(), any());
        verify(propertyRevisionRepository, never()).save(any());
        verify(property, never()).update(any(), any());
        verifyNoInteractions(entityManager);
        verifyNoDomainEventsRecorded();
    }

    @Test
    void update_optionsNoOpWithTitleChange_omitsOptionsAndSkipsSynchronization() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION,
                Map.of("title", JsonMapper.builder().build().valueToTree(AFTER_TITLE)), null, null, List.of());
        prepareOptionUpdate(request);
        when(propertyOptionCommandService.prepareSync(PROPERTY_ID, PropertyType.APARTMENT, List.of()))
                .thenReturn(new OptionSyncPlan(false));

        propertyUpdateService.update(PROPERTY_ID, request, ownerContext);

        verify(objectMapper).writeValueAsString(List.of("title"));
        verify(propertyOptionCommandService, never()).synchronizeOptions(any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_nullOptions_keepsOptionsUntouched() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION,
                Map.of("title", JsonMapper.builder().build().valueToTree(AFTER_TITLE)));
        prepareOptionUpdate(request);
        propertyUpdateService.update(PROPERTY_ID, request, ownerContext);
        verifyNoInteractions(propertyOptionCommandService);
    }

    @Test
    void update_actualPropertyTypeChangeWithoutOptions_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION,
                Map.of("propertyType", JsonMapper.builder().build().valueToTree("OFFICETEL")));
        Property property = prepareOptionUpdate(request);

        assertThatThrownBy(() -> propertyUpdateService.update(PROPERTY_ID, request, ownerContext))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.INVALID_REQUEST);
                    assertThat(exception.getMessage()).contains("매물 유형 변경 시 옵션 목록을 함께 전달해야 합니다.");
                });
        verifyNoInteractions(propertyOptionCommandService);
        verify(property, never()).update(any(), any());
        verify(propertyRevisionRepository, never()).save(any());
    }

    @Test
    void update_unchangedPropertyTypeWithoutOptions_allowsOtherChanges() {
        JsonMapper mapper = JsonMapper.builder().build();
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION,
                Map.of("propertyType", mapper.valueToTree("APARTMENT"), "title", mapper.valueToTree(AFTER_TITLE)));
        prepareOptionUpdate(request);
        propertyUpdateService.update(PROPERTY_ID, request, ownerContext);
        verifyNoInteractions(propertyOptionCommandService);
        verify(objectMapper).writeValueAsString(List.of("title"));
    }

    @Test
    void update_propertyTypeWithOptions_usesFinalTypeAndSortedChangedFields() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(CURRENT_VERSION,
                Map.of("propertyType", JsonMapper.builder().build().valueToTree("OFFICETEL")),
                null, null, List.of(new PropertyOptionRequest("PARKING", "2")));
        Property property = prepareOptionUpdate(request);
        List<PropertyOptionCreateCommand> commands = List.of(new PropertyOptionCreateCommand("PARKING", "2"));
        when(propertyOptionCommandService.prepareSync(PROPERTY_ID, PropertyType.OFFICETEL, commands))
                .thenReturn(new OptionSyncPlan(true));

        propertyUpdateService.update(PROPERTY_ID, request, ownerContext);

        verify(propertyOptionCommandService).prepareSync(PROPERTY_ID, PropertyType.OFFICETEL, commands);
        verify(propertyOptionCommandService).synchronizeOptions(
                PROPERTY_ID, 999L, PropertyType.OFFICETEL, commands, "options", ownerContext);
        verify(objectMapper).writeValueAsString(List.of("options", "propertyType"));
        verify(property).update(argThat(command -> command.propertyType() == PropertyType.OFFICETEL), eq(ownerContext));
        verifyNoInteractions(entityManager);
    }

    private Property prepareOptionUpdate(PropertyUpdateRequest request) {
        Property property = prepareExistingProperty(CURRENT_VERSION);
        prepareCompletePropertyState(property);
        when(property.getTitle()).thenReturn(BEFORE_TITLE);
        PropertyUpdateCommand command = new PropertyUpdateCommandFactory(JsonMapper.builder().build())
                .create(property, request);
        when(propertyUpdateCommandFactory.create(property, request, null)).thenReturn(command);
        when(regionRepository.findByRegionIdAndIsActiveTrue(REGION_ID)).thenReturn(Optional.of(mock(Region.class)));
        when(propertyRepository.saveAndFlush(property)).thenReturn(property);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        PropertyRevision savedRevision = mock(PropertyRevision.class);
        when(savedRevision.getPropertyRevisionId()).thenReturn(999L);
        when(propertyRevisionRepository.save(any(PropertyRevision.class))).thenReturn(savedRevision);
        return property;
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
        ).thenReturn(Optional.of(property));

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getAuthorMemberId())
                .thenReturn(AUTHOR_MEMBER_ID);

        when(property.getVersion())
                .thenReturn(currentVersion);

        when(property.getPropertyType()).thenReturn(PropertyType.APARTMENT);

        return property;
    }

    private void prepareCompletePropertyState(
            Property property
    ) {
        when(property.getRegionId())
                .thenReturn(REGION_ID);

        when(property.getApartmentComplexId())
                .thenReturn(null);

        when(property.getPublisherType())
                .thenReturn(PublisherType.DIRECT_OWNER);

        when(property.getPropertyType())
                .thenReturn(PropertyType.APARTMENT);

        when(property.getTransactionType())
                .thenReturn(TransactionType.SALE);

        when(property.getSalePrice())
                .thenReturn(500_000_000L);

        when(property.getDeposit())
                .thenReturn(null);

        when(property.getMonthlyRent())
                .thenReturn(null);

        when(property.getMaintenanceFee())
                .thenReturn(150_000L);

        when(property.getSupplyArea())
                .thenReturn(new BigDecimal("84.99"));

        when(property.getExclusiveArea())
                .thenReturn(new BigDecimal("59.99"));

        when(property.getRoomCount())
                .thenReturn(3);

        when(property.getBathroomCount())
                .thenReturn(1);

        when(property.getFloor())
                .thenReturn(5);

        when(property.getTotalFloor())
                .thenReturn(20);

        when(property.getFloorCondition())
                .thenReturn("중층");

        when(property.getDirection())
                .thenReturn("남향");

        when(property.getApprovalDate())
                .thenReturn(LocalDate.of(2020, 1, 1));

        when(property.getBuildingUse())
                .thenReturn("공동주택");

        when(property.getIsParkingAvailable())
                .thenReturn(true);

        when(property.getHasElevator())
                .thenReturn(true);

        when(property.getIsPetAllowed())
                .thenReturn(false);

        when(property.getDescription())
                .thenReturn("자동 테스트용 매물 설명");

        when(property.getPublicationStatus())
                .thenReturn(PublicationStatus.IN_REVIEW);

        when(property.getTransactionStatus())
                .thenReturn(TransactionStatus.AVAILABLE);

        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);
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

    private PropertyUpdateRequest createTitleUpdateRequest(
            Long requestedVersion
    ) {
        JsonNode titleNode =
                mock(JsonNode.class);

        return new PropertyUpdateRequest(
                requestedVersion,
                Map.of(
                        "title",
                        titleNode
                )
        );
    }

    private PropertyUpdateCommand createUpdateCommand(
            Long requestedVersion,
            String title
    ) {
        return new PropertyUpdateCommand(
                requestedVersion,
                REGION_ID,
                null,
                PropertyType.APARTMENT,
                TransactionType.SALE,
                500_000_000L,
                null,
                null,
                150_000L,
                new BigDecimal("84.99"),
                new BigDecimal("59.99"),
                3,
                1,
                5,
                20,
                "중층",
                "남향",
                LocalDate.of(2020, 1, 1),
                "공동주택",
                true,
                true,
                false,
                title,
                "자동 테스트용 매물 설명"
        );
    }

    private void prepareImageSnapshots(
            Property property,
            JsonNode fileIdsNode
    ) {
        ObjectNode snapshot = mock(ObjectNode.class);
        when(objectMapper.valueToTree(property)).thenReturn(snapshot);
        when(objectMapper.valueToTree(any(List.class)))
                .thenReturn(fileIdsNode);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(
                "{\"title\":\"수정 전 제목\",\"fileIds\":[1,2]}",
                "{\"title\":\"수정 후 제목\",\"fileIds\":[2,3]}"
        );
    }
}
