package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PropertyUpdateCommandFactory {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "regionId",
            "apartmentComplexId",
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
            "description"
    );

    private final ObjectMapper objectMapper;

    public PropertyUpdateCommand create(
            Property property,
            PropertyUpdateRequest request
    ) {
        return create(property, request, null);
    }

    public PropertyUpdateCommand create(
            Property property,
            PropertyUpdateRequest request,
            Long verifiedRegionId
    ) {
        Map<String, JsonNode> changes = request.changes();

        validateChanges(changes, request.isUpdateTargetProvided());
        validateRegionChange(
                changes,
                request.address() != null,
                verifiedRegionId
        );

        return new PropertyUpdateCommand(
                request.version(),
                verifiedRegionId != null
                        ? verifiedRegionId
                        : valueOrCurrent(
                                changes,
                                "regionId",
                                Long.class,
                                property.getRegionId()
                        ),
                valueOrCurrent(
                        changes,
                        "apartmentComplexId",
                        Long.class,
                        property.getApartmentComplexId()
                ),
                valueOrCurrent(
                        changes,
                        "propertyType",
                        PropertyType.class,
                        property.getPropertyType()
                ),
                valueOrCurrent(
                        changes,
                        "transactionType",
                        TransactionType.class,
                        property.getTransactionType()
                ),
                valueOrCurrent(
                        changes,
                        "salePrice",
                        Long.class,
                        property.getSalePrice()
                ),
                valueOrCurrent(
                        changes,
                        "deposit",
                        Long.class,
                        property.getDeposit()
                ),
                valueOrCurrent(
                        changes,
                        "monthlyRent",
                        Long.class,
                        property.getMonthlyRent()
                ),
                valueOrCurrent(
                        changes,
                        "maintenanceFee",
                        Long.class,
                        property.getMaintenanceFee()
                ),
                valueOrCurrent(
                        changes,
                        "supplyArea",
                        BigDecimal.class,
                        property.getSupplyArea()
                ),
                valueOrCurrent(
                        changes,
                        "exclusiveArea",
                        BigDecimal.class,
                        property.getExclusiveArea()
                ),
                valueOrCurrent(
                        changes,
                        "roomCount",
                        Integer.class,
                        property.getRoomCount()
                ),
                valueOrCurrent(
                        changes,
                        "bathroomCount",
                        Integer.class,
                        property.getBathroomCount()
                ),
                valueOrCurrent(
                        changes,
                        "floor",
                        Integer.class,
                        property.getFloor()
                ),
                valueOrCurrent(
                        changes,
                        "totalFloor",
                        Integer.class,
                        property.getTotalFloor()
                ),
                valueOrCurrent(
                        changes,
                        "floorCondition",
                        String.class,
                        property.getFloorCondition()
                ),
                valueOrCurrent(
                        changes,
                        "direction",
                        String.class,
                        property.getDirection()
                ),
                valueOrCurrent(
                        changes,
                        "approvalDate",
                        LocalDate.class,
                        property.getApprovalDate()
                ),
                valueOrCurrent(
                        changes,
                        "buildingUse",
                        String.class,
                        property.getBuildingUse()
                ),
                valueOrCurrent(
                        changes,
                        "isParkingAvailable",
                        Boolean.class,
                        property.getIsParkingAvailable()
                ),
                valueOrCurrent(
                        changes,
                        "hasElevator",
                        Boolean.class,
                        property.getHasElevator()
                ),
                valueOrCurrent(
                        changes,
                        "isPetAllowed",
                        Boolean.class,
                        property.getIsPetAllowed()
                ),
                valueOrCurrent(
                        changes,
                        "title",
                        String.class,
                        property.getTitle()
                ),
                valueOrCurrent(
                        changes,
                        "description",
                        String.class,
                        property.getDescription()
                )
        );
    }

    private void validateChanges(
            Map<String, JsonNode> changes,
            boolean updateTargetProvided
    ) {
        if (
                !updateTargetProvided
        ) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "수정할 필드, 파일 ID 목록, 주소 또는 옵션 목록이 필요합니다."
            );
        }

        List<String> invalidFields = changes.keySet()
                .stream()
                .filter(field -> !ALLOWED_FIELDS.contains(field))
                .sorted()
                .toList();

        if (!invalidFields.isEmpty()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "수정할 수 없는 필드가 포함되어 있습니다: "
                            + String.join(", ", invalidFields)
            );
        }
    }

    private void validateRegionChange(
            Map<String, JsonNode> changes,
            boolean addressChangeRequested,
            Long verifiedRegionId
    ) {
        if (!changes.containsKey("regionId")) {
            return;
        }

        if (!addressChangeRequested) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "regionId는 정확주소와 함께 변경해야 합니다."
            );
        }

        Long requestedRegionId = valueOrCurrent(
                changes,
                "regionId",
                Long.class,
                null
        );

        if (!Objects.equals(requestedRegionId, verifiedRegionId)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "요청 regionId와 주소·좌표로 검증된 Region이 일치하지 않습니다."
            );
        }
    }

    private <T> T valueOrCurrent(
            Map<String, JsonNode> changes,
            String fieldName,
            Class<T> targetType,
            T currentValue
    ) {
        if (!changes.containsKey(fieldName)) {
            return currentValue;
        }

        JsonNode valueNode = changes.get(fieldName);

        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        try {
            return objectMapper.convertValue(
                    valueNode,
                    targetType
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    fieldName + " 필드의 형식이 올바르지 않습니다."
            );
        }
    }
}
