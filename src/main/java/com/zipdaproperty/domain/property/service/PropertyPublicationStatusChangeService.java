package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
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
public class PropertyPublicationStatusChangeService {

    private static final String PUBLICATION_STATUS_FIELD =
            "publicationStatus";

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final PropertyStatusHistoryRepository
            propertyStatusHistoryRepository;

    private final PropertyVersionPolicy propertyVersionPolicy;

    private final PublicationStatusPolicy publicationStatusPolicy;

    private final ObjectMapper objectMapper;

    private final PropertyAuditEventRecorder
            propertyAuditEventRecorder;

    private final PropertyKafkaEventPublisher
            propertyKafkaEventPublisher;

    @Transactional
    public PropertyPublicationStatusChangeResponse change(
            Long propertyId,
            PropertyPublicationStatusChangeRequest request,
            ActorContext actorContext
    ) {
        Property property = findProperty(propertyId);

        PublicationStatus beforeStatus =
                property.getPublicationStatus();

        validateChangePermission(
                property,
                beforeStatus,
                request.targetStatus(),
                actorContext
        );

        propertyVersionPolicy.validate(
                property.getVersion(),
                request.version()
        );

        publicationStatusPolicy.validateTransition(
                beforeStatus,
                request.targetStatus(),
                property.getTransactionStatus()
        );

        String beforeSnapshotJson =
                objectMapper.writeValueAsString(property);

        Instant occurredAt = Instant.now();

        property.changePublicationStatus(
                request.targetStatus(),
                actorContext,
                occurredAt
        );

        Property savedProperty = saveAndFlush(property);

        String afterSnapshotJson =
                objectMapper.writeValueAsString(savedProperty);

        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        List.of(PUBLICATION_STATUS_FIELD)
                );

        PropertyRevision revision =
                PropertyRevision.statusChanged(
                        savedProperty.getPropertyId(),
                        savedProperty.getVersion(),
                        changedFieldsJson,
                        beforeSnapshotJson,
                        afterSnapshotJson,
                        request.reason(),
                        actorContext,
                        occurredAt
                );

        PropertyRevision savedRevision =
                propertyRevisionRepository.saveAndFlush(revision);

        PropertyStatusHistory statusHistory =
                new PropertyStatusHistory(
                        savedProperty.getPropertyId(),
                        savedRevision.getPropertyRevisionId(),
                        PropertyStatusType.PUBLICATION,
                        beforeStatus.name(),
                        savedProperty.getPublicationStatus().name(),
                        null,
                        request.reason(),
                        savedProperty.getVersion(),
                        occurredAt,
                        actorContext
                );

        propertyStatusHistoryRepository.save(statusHistory);

        recordPublicationStatusChangeEvents(
                savedProperty,
                beforeStatus,
                request.reason(),
                occurredAt,
                actorContext
        );

        return PropertyPublicationStatusChangeResponse.from(
                savedProperty
        );
    }

    private Property findProperty(Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            throw propertyNotFound();
        }

        return propertyRepository
                .findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(this::propertyNotFound);
    }

    private BusinessException propertyNotFound() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_NOT_FOUND,
                "매물을 찾을 수 없습니다."
        );
    }

    private void validateChangePermission(
            Property property,
            PublicationStatus beforeStatus,
            PublicationStatus targetStatus,
            ActorContext actorContext
    ) {
        if (
                actorContext == null
                        || !actorContext.isMemberRequest()
        ) {
            throw permissionDenied();
        }

        boolean isOwner =
                isOwner(property, actorContext);

        boolean isAdmin =
                isAllowedAdmin(actorContext.role());

        boolean isReviewDecision =
                beforeStatus == PublicationStatus.IN_REVIEW
                        && (
                        targetStatus == PublicationStatus.PUBLISHED
                                || targetStatus == PublicationStatus.REJECTED
                );

        boolean isResubmission =
                beforeStatus == PublicationStatus.REJECTED
                        && targetStatus == PublicationStatus.IN_REVIEW;

        if (isReviewDecision && !isAdmin) {
            throw permissionDenied();
        }

        if (isResubmission && !isOwner) {
            throw permissionDenied();
        }

        if (!isReviewDecision && !isResubmission
                && !isOwner && !isAdmin) {
            throw permissionDenied();
        }
    }

    private boolean isOwner(
            Property property,
            ActorContext actorContext
    ) {
        ActorRole actorRole = actorContext.role();

        return (
                actorRole == ActorRole.USER
                        || actorRole == ActorRole.AGENT
        )
                && Objects.equals(
                property.getAuthorMemberId(),
                actorContext.memberId()
        );
    }

    private boolean isAllowedAdmin(ActorRole actorRole) {
        return actorRole == ActorRole.CS_ADMIN
                || actorRole == ActorRole.SUPER_ADMIN;
    }

    private BusinessException permissionDenied() {
        return new BusinessException(
                CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                "해당 공개 상태를 변경할 권한이 없습니다."
        );
    }

    private Property saveAndFlush(Property property) {
        try {
            return propertyRepository.saveAndFlush(property);
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 사용자가 먼저 매물 공개 상태를 변경했습니다."
            );
        }
    }

    private void recordPublicationStatusChangeEvents(
            Property property,
            PublicationStatus beforeStatus,
            String reason,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        propertyAuditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode
                        .PROPERTY_PUBLICATION_STATUS_CHANGED,
                reason,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> kafkaPayload =
                createPublicationStatusChangedPayload(
                        property,
                        beforeStatus,
                        reason
                );

        propertyKafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                resolveKafkaEventType(
                        property.getPublicationStatus()
                ),
                kafkaPayload,
                occurredAt,
                actorContext
        );
    }

    private Map<String, Object>
    createPublicationStatusChangedPayload(
            Property property,
            PublicationStatus beforeStatus,
            String reason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put(
                "propertyId",
                property.getPropertyId().toString()
        );
        payload.put("version", property.getVersion());
        payload.put(
                "beforePublicationStatus",
                beforeStatus.name()
        );
        payload.put(
                "publicationStatus",
                property.getPublicationStatus().name()
        );
        payload.put("reason", reason);

        return payload;
    }

    private String resolveKafkaEventType(
            PublicationStatus publicationStatus
    ) {
        return switch (publicationStatus) {
            case PUBLISHED -> PropertyEventType.PROPERTY_PUBLISHED;
            case HIDDEN -> PropertyEventType.PROPERTY_HIDDEN;
            case REJECTED -> PropertyEventType.PROPERTY_REJECTED;
            case IN_REVIEW -> PropertyEventType.PROPERTY_RESUBMITTED;
        };
    }
}
