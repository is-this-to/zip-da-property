package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyRestoreRequest;
import com.zipdaproperty.domain.property.response.PropertyRestoreResponse;
import com.zipdaproperty.domain.region.repository.RegionRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PropertyRestoreService {

    private static final List<String> RESTORE_CHANGED_FIELDS =
            List.of(
                    "deletedAt",
                    "deletedByMemberId",
                    "deletedByRole",
                    "deleteReason"
            );

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final RegionRepository regionRepository;

    private final PropertyVersionPolicy propertyVersionPolicy;

    private final ObjectMapper objectMapper;

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder;

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher;

    @Transactional
    public PropertyRestoreResponse restore(
            Long propertyId,
            PropertyRestoreRequest request,
            ActorContext actorContext
    ) {
        Property property =
                findDeletedProperty(propertyId);

        validateRestorePermission(actorContext);

        propertyVersionPolicy.validate(
                property.getVersion(),
                request.version()
        );

        validateRestoreReference(property);

        String beforeSnapshotJson =
                objectMapper.writeValueAsString(property);

        Instant occurredAt = Instant.now();

        property.restore(actorContext);

        Property savedProperty =
                saveAndFlush(property);

        String afterSnapshotJson =
                objectMapper.writeValueAsString(
                        savedProperty
                );

        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        RESTORE_CHANGED_FIELDS
                );

        PropertyRevision revision =
                PropertyRevision.restored(
                        savedProperty.getPropertyId(),
                        savedProperty.getVersion(),
                        changedFieldsJson,
                        beforeSnapshotJson,
                        afterSnapshotJson,
                        request.restoreReason(),
                        actorContext,
                        occurredAt
                );

        propertyRevisionRepository.save(
                revision
        );

        recordRestoreEvents(
                savedProperty,
                request.restoreReason(),
                occurredAt,
                actorContext
        );

        return PropertyRestoreResponse.from(
                savedProperty
        );
    }

    private Property findDeletedProperty(
            Long propertyId
    ) {
        if (propertyId == null || propertyId <= 0) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    "매물을 찾을 수 없습니다."
            );
        }

        return propertyRepository
                .findByPropertyIdAndDeletedAtIsNotNull(
                        propertyId
                )
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode
                                        .PROPERTY_NOT_FOUND,
                                "복구할 삭제 매물을 찾을 수 없습니다. "
                                        + "propertyId = "
                                        + propertyId
                        )
                );
    }

    private void validateRestorePermission(
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "회원 관리자 요청만 매물을 복구할 수 있습니다."
            );
        }

        ActorRole actorRole =
                actorContext.role();

        boolean isAllowedAdmin =
                actorRole == ActorRole.CS_ADMIN
                        || actorRole == ActorRole.SUPER_ADMIN;

        if (!isAllowedAdmin) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "CS_ADMIN 또는 SUPER_ADMIN만 "
                            + "삭제된 매물을 복구할 수 있습니다."
            );
        }
    }

    private void validateRestoreReference(
            Property property
    ) {
        Long regionId =
                property.getRegionId();

        boolean activeRegionExists =
                regionId != null
                        && regionRepository
                        .findByRegionIdAndIsActiveTrue(
                                regionId
                        )
                        .isPresent();

        if (!activeRegionExists) {
            throw new BusinessException(
                    CustomResponseCode
                            .RESTORE_REFERENCE_INVALID,
                    "활성 Region을 찾을 수 없어 "
                            + "매물을 복구할 수 없습니다."
            );
        }
    }

    private Property saveAndFlush(
            Property property
    ) {
        try {
            return propertyRepository
                    .saveAndFlush(property);
        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 사용자가 먼저 매물을 변경했습니다."
            );
        }
    }

    private void recordRestoreEvents(
            Property property,
            String restoreReason,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        propertyAuditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_RESTORED,
                restoreReason,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> kafkaPayload =
                createPropertyReactivatedPayload(
                        property,
                        restoreReason,
                        actorContext
                );

        propertyKafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                PropertyEventType.PROPERTY_REACTIVATED,
                kafkaPayload,
                occurredAt,
                actorContext
        );
    }

    private Map<String, Object> createPropertyReactivatedPayload(
            Property property,
            String restoreReason,
            ActorContext actorContext
    ) {
        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put(
                "propertyId",
                property.getPropertyId().toString()
        );

        payload.put(
                "version",
                property.getVersion()
        );

        payload.put(
                "deletedAt",
                property.getDeletedAt()
        );

        payload.put(
                "restoredByMemberId",
                toStringOrNull(
                        actorContext.memberId()
                )
        );

        payload.put(
                "restoredByRole",
                enumNameOrNull(
                        actorContext.role()
                )
        );

        payload.put(
                "restoreReason",
                restoreReason
        );

        payload.put(
                "publicationStatus",
                enumNameOrNull(
                        property.getPublicationStatus()
                )
        );

        payload.put(
                "transactionStatus",
                enumNameOrNull(
                        property.getTransactionStatus()
                )
        );

        return payload;
    }

    private String toStringOrNull(Long value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private String enumNameOrNull(Enum<?> value) {
        if (value == null) {
            return null;
        }

        return value.name();
    }
}
