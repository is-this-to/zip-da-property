package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageSyncService;
import com.zipdaproperty.domain.image.service.PropertyImageSyncService.SyncPlan;
import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.option.service.PropertyOptionCommandService;
import com.zipdaproperty.domain.option.service.PropertyOptionCommandService.OptionSyncPlan;
import com.zipdaproperty.domain.property.request.PropertyOptionRequest;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.model.PreparedPropertyAddress;
import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.domain.property.member.service.MemberWritePermissionService;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.domain.property.response.PropertyUpdateResponse;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class PropertyUpdateService {

    private static final String UPDATE_REASON =
            "매물 핵심 정보가 수정되었습니다.";

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final RegionRepository regionRepository;

    private final PropertyVersionPolicy propertyVersionPolicy;

    private final PropertyUpdateCommandFactory
            propertyUpdateCommandFactory;

    private final PropertyUpdatePolicy propertyUpdatePolicy;

    private final PropertyPricePolicy propertyPricePolicy;

    private final PropertyImageSyncService propertyImageSyncService;

    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder;

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher;

    private final PropertyAddressService propertyAddressService;

    private final PropertyOptionCommandService propertyOptionCommandService;

    private final MemberWritePermissionService
            memberWritePermissionService;

    @Transactional
    public PropertyUpdateResponse update(
            Long propertyId,
            PropertyUpdateRequest request,
            ActorContext actorContext
    ) {
        Property property = findProperty(propertyId);

        validateUpdatePermission(
                property,
                actorContext
        );

        validateMemberPermission(actorContext);

        Long currentPropertyVersion = property.getVersion();

        propertyVersionPolicy.validate(
                currentPropertyVersion,
                request.version()
        );

        PreparedPropertyAddress preparedAddress =
                request.address() == null
                        ? null
                        : propertyAddressService.prepare(
                                propertyId,
                                request.address().toCommand()
                        );

        PropertyUpdateCommand command =
                propertyUpdateCommandFactory.create(
                        property,
                        request,
                        preparedAddress == null
                                ? null
                                : preparedAddress.regionId()
                );

        if (property.getPropertyType() != command.propertyType()
                && request.options() == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "매물 유형 변경 시 옵션 목록을 함께 전달해야 합니다."
            );
        }

        propertyUpdatePolicy.validate(command);

        validateRegion(command.regionId());

        propertyPricePolicy.validate(
                command.transactionType(),
                command.salePrice(),
                command.deposit(),
                command.monthlyRent()
        );

        boolean imageChangeRequested = request.fileIds() != null;
        SyncPlan imageSyncPlan = imageChangeRequested
                ? propertyImageSyncService.prepareSync(
                        propertyId,
                        request.fileIds()
                )
                : null;

        List<PropertyOptionCreateCommand> optionCommands = request.options() == null
                ? null
                : request.options().stream().map(PropertyOptionRequest::toCommand).toList();
        OptionSyncPlan optionSyncPlan = optionCommands == null
                ? null
                : propertyOptionCommandService.prepareSync(
                        propertyId,
                        command.propertyType(),
                        optionCommands
                );

        List<String> propertyChangedFields =
                detectChangedFields(
                        property,
                        command,
                        request.changes().keySet(),
                        preparedAddress != null
                );
        List<String> changedFields = mergeChangedFields(
                propertyChangedFields,
                imageSyncPlan,
                optionSyncPlan
        );

        validateActualChanges(changedFields);

        String beforeSnapshotJson = writeSnapshot(
                property,
                imageSyncPlan == null
                        ? null
                        : imageSyncPlan.currentFileIds()
        );

        Instant occurredAt = Instant.now();

        if (propertyChangedFields.isEmpty()) {
            entityManager.lock(
                    property,
                    LockModeType.OPTIMISTIC_FORCE_INCREMENT
            );
        } else {
            property.update(
                    command,
                    actorContext
            );
        }

        if (imageSyncPlan != null
                && imageSyncPlan.changesRequired()) {
            propertyImageSyncService.syncImages(
                    propertyId,
                    request.fileIds(),
                    actorContext
            );
        }

        boolean addressOnlyChange =
                changedFields.size() == 1
                        && changedFields.contains("address");

        Property savedProperty = saveAndFlush(
                property,
                addressOnlyChange
        );

        if (preparedAddress != null) {
            propertyAddressService.change(
                    savedProperty,
                    preparedAddress,
                    actorContext
            );
        }

        Long resultingPropertyVersion =
                propertyChangedFields.isEmpty()
                        ? currentPropertyVersion + 1
                        : savedProperty.getVersion();

        String afterSnapshotJson = writeSnapshot(
                savedProperty,
                imageSyncPlan == null
                        ? null
                        : request.fileIds()
        );

        String changedFieldsJson =
                objectMapper.writeValueAsString(changedFields);

        PropertyRevision revision =
                PropertyRevision.updated(
                        savedProperty.getPropertyId(),
                        resultingPropertyVersion,
                        changedFieldsJson,
                        beforeSnapshotJson,
                        afterSnapshotJson,
                        UPDATE_REASON,
                        actorContext,
                        occurredAt
                );

        PropertyRevision savedRevision = propertyRevisionRepository.save(revision);

        if (optionSyncPlan != null && optionSyncPlan.changesRequired()) {
            propertyOptionCommandService.synchronizeOptions(
                    propertyId,
                    savedRevision.getPropertyRevisionId(),
                    command.propertyType(),
                    optionCommands,
                    "options",
                    actorContext
            );
        }

        recordUpdateEvents(
                savedProperty,
                resultingPropertyVersion,
                changedFields,
                actorContext,
                occurredAt
        );

        return PropertyUpdateResponse.from(
                savedProperty,
                resultingPropertyVersion
        );
    }

    private List<String> mergeChangedFields(
            List<String> propertyChangedFields,
            SyncPlan imageSyncPlan,
            OptionSyncPlan optionSyncPlan
    ) {
        List<String> changedFields = new ArrayList<>(
                propertyChangedFields
        );
        if (imageSyncPlan != null
                && imageSyncPlan.changesRequired()) {
            changedFields.add("fileIds");
        }
        if (optionSyncPlan != null && optionSyncPlan.changesRequired()) {
            changedFields.add("options");
        }
        return changedFields.stream().sorted().toList();
    }

    private String writeSnapshot(
            Property property,
            List<Long> fileIds
    ) {
        if (fileIds == null) {
            return objectMapper.writeValueAsString(property);
        }

        ObjectNode snapshot = objectMapper.valueToTree(property);
        snapshot.set(
                "fileIds",
                objectMapper.valueToTree(fileIds)
        );
        return objectMapper.writeValueAsString(snapshot);
    }

    private Property findProperty(Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    "매물을 찾을 수 없습니다."
            );
        }

        return propertyRepository
                .findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode.PROPERTY_NOT_FOUND,
                                "매물을 찾을 수 없습니다. propertyId = "
                                        + propertyId
                        )
                );
    }

    private void validateUpdatePermission(
            Property property,
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "회원 요청만 매물을 수정할 수 있습니다."
            );
        }

        ActorRole actorRole = actorContext.role();

        boolean isOwner =
                (
                        actorRole == ActorRole.USER
                                || actorRole == ActorRole.AGENT
                )
                        && Objects.equals(
                        property.getAuthorMemberId(),
                        actorContext.memberId()
                );

        boolean isAllowedAdmin =
                actorRole == ActorRole.CS_ADMIN
                        || actorRole == ActorRole.SUPER_ADMIN;

        if (!isOwner && !isAllowedAdmin) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "매물 작성자 또는 허용된 관리자만 수정할 수 있습니다."
            );
        }
    }

    private void validateMemberPermission(
            ActorContext actorContext
    ) {
        if (actorContext.role() != ActorRole.USER
                && actorContext.role() != ActorRole.AGENT) {
            return;
        }

        memberWritePermissionService.validate(
                actorContext.memberId(),
                actorContext.role(),
                MemberPermissionAction.PROPERTY_UPDATE
        );
    }

    private void validateRegion(Long regionId) {
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

    private List<String> detectChangedFields(
            Property property,
            PropertyUpdateCommand command,
            Set<String> requestedFields,
            boolean addressChangeRequested
    ) {
        Set<String> changedFields = requestedFields
                .stream()
                .filter(
                        fieldName -> isActuallyChanged(
                                fieldName,
                                property,
                                command
                        )
                )
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        if (
                !Objects.equals(
                        property.getRegionId(),
                        command.regionId()
                )
        ) {
            changedFields.add("regionId");
        }

        if (addressChangeRequested) {
            changedFields.add("address");
        }

        return List.copyOf(changedFields);
    }

    private boolean isActuallyChanged(
            String fieldName,
            Property property,
            PropertyUpdateCommand command
    ) {
        return switch (fieldName) {
            case "regionId" ->
                    !Objects.equals(
                            property.getRegionId(),
                            command.regionId()
                    );

            case "apartmentComplexId" ->
                    !Objects.equals(
                            property.getApartmentComplexId(),
                            command.apartmentComplexId()
                    );

            case "propertyType" ->
                    property.getPropertyType()
                            != command.propertyType();

            case "transactionType" ->
                    property.getTransactionType()
                            != command.transactionType();

            case "salePrice" ->
                    !Objects.equals(
                            property.getSalePrice(),
                            command.salePrice()
                    );

            case "deposit" ->
                    !Objects.equals(
                            property.getDeposit(),
                            command.deposit()
                    );

            case "monthlyRent" ->
                    !Objects.equals(
                            property.getMonthlyRent(),
                            command.monthlyRent()
                    );

            case "maintenanceFee" ->
                    !Objects.equals(
                            property.getMaintenanceFee(),
                            command.maintenanceFee()
                    );

            case "supplyArea" ->
                    isBigDecimalChanged(
                            property.getSupplyArea(),
                            command.supplyArea()
                    );

            case "exclusiveArea" ->
                    isBigDecimalChanged(
                            property.getExclusiveArea(),
                            command.exclusiveArea()
                    );

            case "roomCount" ->
                    !Objects.equals(
                            property.getRoomCount(),
                            command.roomCount()
                    );

            case "bathroomCount" ->
                    !Objects.equals(
                            property.getBathroomCount(),
                            command.bathroomCount()
                    );

            case "floor" ->
                    !Objects.equals(
                            property.getFloor(),
                            command.floor()
                    );

            case "totalFloor" ->
                    !Objects.equals(
                            property.getTotalFloor(),
                            command.totalFloor()
                    );

            case "floorCondition" ->
                    !Objects.equals(
                            property.getFloorCondition(),
                            command.floorCondition()
                    );

            case "direction" ->
                    !Objects.equals(
                            property.getDirection(),
                            command.direction()
                    );

            case "approvalDate" ->
                    !Objects.equals(
                            property.getApprovalDate(),
                            command.approvalDate()
                    );

            case "buildingUse" ->
                    !Objects.equals(
                            property.getBuildingUse(),
                            command.buildingUse()
                    );

            case "isParkingAvailable" ->
                    !Objects.equals(
                            property.getIsParkingAvailable(),
                            command.isParkingAvailable()
                    );

            case "hasElevator" ->
                    !Objects.equals(
                            property.getHasElevator(),
                            command.hasElevator()
                    );

            case "isPetAllowed" ->
                    !Objects.equals(
                            property.getIsPetAllowed(),
                            command.isPetAllowed()
                    );

            case "title" ->
                    !Objects.equals(
                            property.getTitle(),
                            command.title()
                    );

            case "description" ->
                    !Objects.equals(
                            property.getDescription(),
                            command.description()
                    );

            default -> false;
        };
    }

    private boolean isBigDecimalChanged(
            BigDecimal currentValue,
            BigDecimal requestedValue
    ) {
        if (
                currentValue == null
                        && requestedValue == null
        ) {
            return false;
        }

        if (
                currentValue == null
                        || requestedValue == null
        ) {
            return true;
        }

        return currentValue.compareTo(requestedValue) != 0;
    }

    private void validateActualChanges(
            List<String> changedFields
    ) {
        if (changedFields.isEmpty()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "실제로 변경된 필드가 없습니다."
            );
        }
    }

    private Property saveAndFlush(
            Property property,
            boolean forceVersionIncrement
    ) {
        try {
            if (forceVersionIncrement) {
                entityManager.lock(
                        property,
                        LockModeType.PESSIMISTIC_FORCE_INCREMENT
                );
                entityManager.flush();
                return property;
            }

            return propertyRepository.saveAndFlush(property);
        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 사용자가 먼저 매물을 수정했습니다."
            );
        }
    }

    private void recordUpdateEvents(
            Property property,
            Long propertyVersion,
            List<String> changedFields,
            ActorContext actorContext,
            Instant occurredAt
    ) {
        propertyAuditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_UPDATED,
                UPDATE_REASON,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> kafkaPayload =
                createPropertyUpdatedPayload(
                        property,
                        propertyVersion,
                        changedFields
                );

        propertyKafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                propertyVersion,
                PropertyEventType.PROPERTY_UPDATED,
                kafkaPayload,
                occurredAt,
                actorContext
        );
    }

    private Map<String, Object> createPropertyUpdatedPayload(
            Property property,
            Long propertyVersion,
            List<String> changedFields
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put(
                "propertyId",
                property.getPropertyId().toString()
        );
        payload.put(
                "version",
                propertyVersion
        );
        payload.put(
                "changedFields",
                List.copyOf(changedFields)
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
