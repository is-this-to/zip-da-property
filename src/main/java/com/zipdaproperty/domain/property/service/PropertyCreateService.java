package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageLinkService;
import com.zipdaproperty.domain.option.service.PropertyOptionCommandService;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyPublisherSnapshot;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.domain.property.member.service.MemberWritePermissionService;
import com.zipdaproperty.domain.property.repository.PropertyPublisherSnapshotRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PropertyCreateService {

    private static final String CREATE_REASON_CODE =
            "PROPERTY_CREATED";

    private static final String CREATE_REASON =
            "매물 등록으로 초기 상태가 설정되었습니다.";

    private static final List<String> INITIAL_CHANGED_FIELDS = List.of(
            "regionId",
            "apartmentComplexId",
            "authorMemberId",
            "publisherType",  // 매물 주체유형
            "propertyType",
            "transactionType",
            "salePrice",
            "deposit",
            "monthlyRent",
            "maintenanceFee",
            "supplyArea",
            "exclusiveArea",
            "roomCount",
            "bathroomCount",
            "floor",
            "totalFloor",
            "floorCondition",
            "direction",
            "approvalDate",
            "buildingUse",
            "isParkingAvailable",
            "hasElevator",
            "isPetAllowed",
            "title",
            "description",
            "publicationStatus",
            "transactionStatus",
            "verificationStatus"
    );

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final PropertyStatusHistoryRepository
            propertyStatusHistoryRepository;

    private final PropertyPublisherSnapshotRepository
            propertyPublisherSnapshotRepository;

    private final RegionRepository regionRepository;

    private final PropertyPricePolicy propertyPricePolicy;

    private final TsidGenerator tsidGenerator;

    private final ObjectMapper objectMapper;

    private final PropertyImageLinkService propertyImageLinkService;

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder;

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher;

    private final PropertyAddressService propertyAddressService;

    private final MemberWritePermissionService
            memberWritePermissionService;

    private final PropertyOptionCommandService propertyOptionCommandService;

    @Transactional
    public PropertyCreateResponse create(
            PropertyCreateCommand command,
            ActorContext actorContext
    ) {
        validateCreatePermission(
                command.publisherType(),
                actorContext
        );

        memberWritePermissionService.validate(
                actorContext.memberId(),
                actorContext.role(),
                MemberPermissionAction.PROPERTY_CREATE
        );

        validateRegion(command.regionId());

        propertyPricePolicy.validate(
                command.transactionType(),
                command.salePrice(),
                command.deposit(),
                command.monthlyRent()
        );

        Long propertyId = tsidGenerator.generate();

        PreparedPropertyAddress preparedAddress =
                propertyAddressService.prepare(
                        propertyId,
                        command.address()
                );

        validateRegionMatch(
                command.regionId(),
                preparedAddress.regionId()
        );

        Property property = Property.create(
                propertyId,
                command,
                actorContext
        );

        Property savedProperty =
                propertyRepository.saveAndFlush(property);

        propertyImageLinkService.linkImages(
                savedProperty.getPropertyId(),
                command.fileIds(),
                actorContext
        );

        propertyAddressService.create(
                savedProperty,
                preparedAddress,
                actorContext
        );

        Instant occurredAt = Instant.now();

        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        INITIAL_CHANGED_FIELDS
                );

        String afterSnapshotJson =
                objectMapper.writeValueAsString(savedProperty);

        PropertyRevision propertyRevision =
                PropertyRevision.created(
                        savedProperty.getPropertyId(),
                        savedProperty.getVersion(),
                        changedFieldsJson,
                        afterSnapshotJson,
                        actorContext,
                        occurredAt
                );

        PropertyRevision savedRevision =
                propertyRevisionRepository.save(
                        propertyRevision
                );

        propertyOptionCommandService.createOptions(
                savedProperty.getPropertyId(),
                savedRevision.getPropertyRevisionId(),
                savedProperty.getPropertyType(),
                command.options(),
                "options",
                actorContext
        );

        saveInitialStatusHistories(
                savedProperty,
                savedRevision,
                actorContext,
                occurredAt
        );

        savePublisherSnapshot(
                savedProperty,
                savedRevision,
                actorContext,
                occurredAt
        );

        recordCreationEvents(
                savedProperty,
                actorContext,
                occurredAt
        );

        return PropertyCreateResponse.from(savedProperty);
    }

    private void validateCreatePermission(
            PublisherType publisherType,
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
                    "회원 요청만 매물을 등록할 수 있습니다."
            );
        }

        ActorRole actorRole = actorContext.role();

        boolean isAllowed = switch (actorRole) {
            case USER ->
                    publisherType
                            == PublisherType.DIRECT_OWNER
                            || publisherType
                            == PublisherType.DIRECT_TENANT;

            case AGENT ->
                    publisherType
                            == PublisherType.AGENT_BROKERAGE;

            case CS_ADMIN, SALES_ADMIN, SUPER_ADMIN ->
                    false;
        };

        if (!isAllowed) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
                    "요청자 역할과 등록 주체 유형이 일치하지 않습니다."
            );
        }
    }

    private void validateRegion(Long regionId) {
        if (regionId == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "지역 ID는 필수입니다."
            );
        }

        regionRepository
                .findByRegionIdAndIsActiveTrue(regionId)
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode.NOT_FOUND_RESOURCE,
                                "활성 상태의 지역을 찾을 수 없습니다. "
                                        + "regionId = "
                                        + regionId
                        )
                );
    }

    private void validateRegionMatch(
            Long requestedRegionId,
            Long verifiedRegionId
    ) {
        if (!java.util.Objects.equals(
                requestedRegionId,
                verifiedRegionId
        )) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "요청 지역과 주소·좌표로 검증된 지역이 일치하지 않습니다."
            );
        }
    }

    private void saveInitialStatusHistories(
            Property property,
            PropertyRevision propertyRevision,
            ActorContext actorContext,
            Instant occurredAt
    ) {
        PropertyStatusHistory publicationHistory =
                new PropertyStatusHistory(
                        property.getPropertyId(),
                        propertyRevision.getPropertyRevisionId(),
                        PropertyStatusType.PUBLICATION,
                        null,
                        property.getPublicationStatus().name(),
                        CREATE_REASON_CODE,
                        CREATE_REASON,
                        property.getVersion(),
                        occurredAt,
                        actorContext
                );

        PropertyStatusHistory transactionHistory =
                new PropertyStatusHistory(
                        property.getPropertyId(),
                        propertyRevision.getPropertyRevisionId(),
                        PropertyStatusType.TRANSACTION,
                        null,
                        property.getTransactionStatus().name(),
                        CREATE_REASON_CODE,
                        CREATE_REASON,
                        property.getVersion(),
                        occurredAt,
                        actorContext
                );

        PropertyStatusHistory verificationHistory =
                new PropertyStatusHistory(
                        property.getPropertyId(),
                        propertyRevision.getPropertyRevisionId(),
                        PropertyStatusType.VERIFICATION,
                        null,
                        property.getVerificationStatus().name(),
                        CREATE_REASON_CODE,
                        CREATE_REASON,
                        property.getVersion(),
                        occurredAt,
                        actorContext
                );

        propertyStatusHistoryRepository.saveAll(
                List.of(
                        publicationHistory,
                        transactionHistory,
                        verificationHistory
                )
        );
    }

    private void savePublisherSnapshot(
            Property property,
            PropertyRevision propertyRevision,
            ActorContext actorContext,
            Instant occurredAt
    ) {
        String snapshotJson =
                objectMapper.writeValueAsString(
                        Map.of(
                                "publisherType",
                                property
                                        .getPublisherType()
                                        .name(),
                                "publisherMemberId",
                                actorContext.memberId()
                        )
                );

        PropertyPublisherSnapshot publisherSnapshot =
                new PropertyPublisherSnapshot(
                        property.getPropertyId(),
                        propertyRevision.getPropertyRevisionId(),
                        property.getPublisherType(),
                        actorContext.memberId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        snapshotJson,
                        occurredAt,
                        actorContext
                );

        propertyPublisherSnapshotRepository.save(
                publisherSnapshot
        );
    }

    private void recordCreationEvents(
            Property property,
            ActorContext actorContext,
            Instant occurredAt
    ) {
        propertyAuditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_CREATED,
                CREATE_REASON,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> kafkaPayload =
                createPropertyCreatedPayload(property);

        propertyKafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                PropertyEventType.PROPERTY_CREATED,
                kafkaPayload,
                occurredAt,
                actorContext
        );
    }

    private Map<String, Object> createPropertyCreatedPayload(
            Property property
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put(
                "propertyId",
                property.getPropertyId().toString()
        );

        payload.put(
                "version",
                property.getVersion()
        );

        payload.put(
                "regionId",
                property.getRegionId().toString()
        );

        payload.put(
                "apartmentComplexId",
                toStringOrNull(
                        property.getApartmentComplexId()
                )
        );

        payload.put(
                "authorMemberId",
                property.getAuthorMemberId().toString()
        );

        payload.put(
                "publisherType",
                property.getPublisherType().name()
        );

        payload.put(
                "propertyType",
                property.getPropertyType().name()
        );

        payload.put(
                "transactionType",
                property.getTransactionType().name()
        );

        payload.put(
                "salePrice",
                property.getSalePrice()
        );

        payload.put(
                "deposit",
                property.getDeposit()
        );

        payload.put(
                "monthlyRent",
                property.getMonthlyRent()
        );

        payload.put(
                "maintenanceFee",
                property.getMaintenanceFee()
        );

        payload.put(
                "supplyArea",
                property.getSupplyArea()
        );

        payload.put(
                "exclusiveArea",
                property.getExclusiveArea()
        );

        payload.put(
                "roomCount",
                property.getRoomCount()
        );

        payload.put(
                "bathroomCount",
                property.getBathroomCount()
        );

        payload.put(
                "floor",
                property.getFloor()
        );

        payload.put(
                "totalFloor",
                property.getTotalFloor()
        );

        payload.put(
                "floorCondition",
                property.getFloorCondition()
        );

        payload.put(
                "direction",
                property.getDirection()
        );

        payload.put(
                "approvalDate",
                property.getApprovalDate()
        );

        payload.put(
                "buildingUse",
                property.getBuildingUse()
        );

        payload.put(
                "isParkingAvailable",
                property.getIsParkingAvailable()
        );

        payload.put(
                "hasElevator",
                property.getHasElevator()
        );

        payload.put(
                "isPetAllowed",
                property.getIsPetAllowed()
        );

        payload.put(
                "title",
                property.getTitle()
        );

        payload.put(
                "description",
                property.getDescription()
        );

        payload.put(
                "publicationStatus",
                property.getPublicationStatus().name()
        );

        payload.put(
                "transactionStatus",
                property.getTransactionStatus().name()
        );

        payload.put(
                "verificationStatus",
                property.getVerificationStatus().name()
        );

        return payload;
    }

    private String toStringOrNull(Long value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }
}
