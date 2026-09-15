package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.image.service.PropertyImageDeletionService;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.request.PropertyDeleteRequest;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyDeleteService {

    private static final List<String> DELETE_CHANGED_FIELDS =
            List.of(
                    "deletedAt",
                    "deletedByMemberId",
                    "deletedByRole",
                    "deleteReason"
            );

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final PropertyVersionPolicy propertyVersionPolicy;

    private final PropertyImageDeletionService propertyImageDeletionService;

    private final ObjectMapper objectMapper;

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder;

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher;

    @Transactional
    public void delete(
            Long propertyId,
            PropertyDeleteRequest request,
            ActorContext actorContext
    ) {
        Property property =
                findActiveProperty(propertyId);

        validateDeletePermission(
                property,
                actorContext
        );

        propertyVersionPolicy.validate(
                property.getVersion(),
                request.version()
        );

        String beforeSnapshotJson =
                objectMapper.writeValueAsString(property);

        Instant occurredAt = Instant.now();

        property.softDelete(
                actorContext,
                occurredAt,
                request.deleteReason()
        );

        propertyImageDeletionService.deleteAllForProperty(
                propertyId,
                actorContext,
                occurredAt
        );

        Property savedProperty =
                saveAndFlush(property);

        String afterSnapshotJson =
                objectMapper.writeValueAsString(
                        savedProperty
                );

        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        DELETE_CHANGED_FIELDS
                );

        PropertyRevision revision =
                PropertyRevision.softDeleted(
                        savedProperty.getPropertyId(),
                        savedProperty.getVersion(),
                        changedFieldsJson,
                        beforeSnapshotJson,
                        afterSnapshotJson,
                        request.deleteReason(),
                        actorContext,
                        occurredAt
                );

        propertyRevisionRepository.save(
                revision
        );

        recordDeleteEvents(
                savedProperty,
                request.deleteReason(),
                occurredAt,
                actorContext
        );
    }

    private Property findActiveProperty(
            Long propertyId
    ) {
        if (propertyId == null || propertyId <= 0) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    "매물을 찾을 수 없습니다."
            );
        }

        return propertyRepository
                .findByPropertyIdAndDeletedAtIsNull(
                        propertyId
                )
                .orElseThrow(
                        () -> new BusinessException(
                                CustomResponseCode
                                        .PROPERTY_NOT_FOUND,
                                "삭제할 활성 매물을 찾을 수 없습니다. "
                                        + "propertyId = "
                                        + propertyId
                        )
                );
    }

    private void validateDeletePermission(
            Property property,
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw new BusinessException(
                    CustomResponseCode
                            .PROPERTY_OWNERSHIP_REQUIRED,
                    "회원 요청만 매물을 삭제할 수 있습니다."
            );
        }

        ActorRole actorRole =
                actorContext.role();

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
                    CustomResponseCode
                            .PROPERTY_OWNERSHIP_REQUIRED,
                    "매물 작성자 또는 허용된 관리자만 "
                            + "매물을 삭제할 수 있습니다."
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

    private void recordDeleteEvents(
            Property property,
            String deleteReason,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        propertyAuditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_SOFT_DELETED,
                deleteReason,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> kafkaPayload =
                createPropertyDeletedPayload(
                        property,
                        deleteReason
                );

        propertyKafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                PropertyEventType.PROPERTY_DELETED,
                kafkaPayload,
                occurredAt,
                actorContext
        );
    }

    private Map<String, Object> createPropertyDeletedPayload(
            Property property,
            String deleteReason
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
                "deletedByMemberId",
                toStringOrNull(
                        property.getDeletedByMemberId()
                )
        );

        payload.put(
                "deletedByRole",
                enumNameOrNull(
                        property.getDeletedByRole()
                )
        );

        payload.put(
                "deleteReason",
                deleteReason
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
