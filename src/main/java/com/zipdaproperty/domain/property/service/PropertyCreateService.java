package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyPublisherSnapshot;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.repository.PropertyPublisherSnapshotRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PropertyCreateService {

    private static final String CREATE_REASON_CODE = "PROPERTY_CREATED";

    private static final String CREATE_REASON =
            "매물 등록으로 초기 상태가 설정되었습니다.";

    private static final List<String> INITIAL_CHANGED_FIELDS = List.of(
            "regionId",
            "apartmentComplexId",
            "authorMemberId",
            "publisherType",
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
    private final PropertyRevisionRepository propertyRevisionRepository;
    private final PropertyStatusHistoryRepository propertyStatusHistoryRepository;
    private final PropertyPublisherSnapshotRepository propertyPublisherSnapshotRepository;
    private final PropertyPricePolicy propertyPricePolicy;
    private final TsidGenerator tsidGenerator;
    private final ObjectMapper objectMapper;

    @Transactional
    public PropertyCreateResponse create(
            PropertyCreateCommand command,
            ActorContext actorContext
    ) {
        validateCreatePermission(
                command.publisherType(),
                actorContext
        );

        propertyPricePolicy.validate(
                command.transactionType(),
                command.salePrice(),
                command.deposit(),
                command.monthlyRent()
        );

        Long propertyId = tsidGenerator.generate();

        Property property = Property.create(
                propertyId,
                command,
                actorContext
        );

        Property savedProperty = propertyRepository.saveAndFlush(property);

        Instant occurredAt = Instant.now();

        String changedFieldsJson =
                objectMapper.writeValueAsString(INITIAL_CHANGED_FIELDS);

        String afterSnapshotJson =
                objectMapper.writeValueAsString(savedProperty);

        PropertyRevision propertyRevision = PropertyRevision.created(
                savedProperty.getPropertyId(),
                savedProperty.getVersion(),
                changedFieldsJson,
                afterSnapshotJson,
                actorContext,
                occurredAt
        );

        PropertyRevision savedRevision =
                propertyRevisionRepository.save(propertyRevision);

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

        return PropertyCreateResponse.from(savedProperty);
    }

    private void validateCreatePermission(
            PublisherType publisherType,
            ActorContext actorContext
    ) {
        if (actorContext == null || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
                    "회원 요청만 매물을 등록할 수 있습니다."
            );
        }

        ActorRole actorRole = actorContext.role();

        boolean isAllowed = switch (actorRole) {
            case USER -> publisherType == PublisherType.DIRECT_OWNER;
            case AGENT -> publisherType == PublisherType.AGENT_BROKERAGE;
            case CS_ADMIN, SALES_ADMIN, SUPER_ADMIN -> false;
        };

        if (!isAllowed) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
                    "요청자 역할과 등록 주체 유형이 일치하지 않습니다."
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
        String snapshotJson = objectMapper.writeValueAsString(
                Map.of(
                        "publisherType",
                        property.getPublisherType().name(),
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

        propertyPublisherSnapshotRepository.save(publisherSnapshot);
    }
}