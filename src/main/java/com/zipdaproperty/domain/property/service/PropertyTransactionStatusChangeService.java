package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.request.PropertyTransactionStatusChangeRequest;
import com.zipdaproperty.domain.property.response.PropertyTransactionStatusChangeResponse;
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
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyTransactionStatusChangeService {

    private static final String TRANSACTION_STATUS_FIELD =
            "transactionStatus";

    private final PropertyRepository propertyRepository;

    private final PropertyRevisionRepository
            propertyRevisionRepository;

    private final PropertyStatusHistoryRepository
            propertyStatusHistoryRepository;

    private final PropertyVersionPolicy propertyVersionPolicy;

    private final TransactionStatusPolicy transactionStatusPolicy;

    private final ObjectMapper objectMapper;

    @Transactional
    public PropertyTransactionStatusChangeResponse change(
            Long propertyId,
            PropertyTransactionStatusChangeRequest request,
            ActorContext actorContext
    ) {
        Property property = findProperty(propertyId);

        validateChangePermission(
                property,
                actorContext
        );

        propertyVersionPolicy.validate(
                property.getVersion(),
                request.version()
        );

        TransactionStatus beforeStatus =
                property.getTransactionStatus();

        transactionStatusPolicy.validateTransition(
                beforeStatus,
                request.targetStatus()
        );

        String beforeSnapshotJson =
                objectMapper.writeValueAsString(property);

        Instant occurredAt = Instant.now();

        property.changeTransactionStatus(
                request.targetStatus(),
                actorContext
        );

        Property savedProperty =
                saveAndFlush(property);

        String afterSnapshotJson =
                objectMapper.writeValueAsString(savedProperty);

        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        List.of(TRANSACTION_STATUS_FIELD)
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
                propertyRevisionRepository.saveAndFlush(
                        revision
                );

        PropertyStatusHistory statusHistory =
                new PropertyStatusHistory(
                        savedProperty.getPropertyId(),
                        savedRevision.getPropertyRevisionId(),
                        PropertyStatusType.TRANSACTION,
                        beforeStatus.name(),
                        savedProperty
                                .getTransactionStatus()
                                .name(),
                        null,
                        request.reason(),
                        savedProperty.getVersion(),
                        occurredAt,
                        actorContext
                );

        propertyStatusHistoryRepository.save(
                statusHistory
        );

        return PropertyTransactionStatusChangeResponse.from(
                savedProperty
        );
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

    private void validateChangePermission(
            Property property,
            ActorContext actorContext
    ) {
        if (actorContext == null
                || !actorContext.isMemberRequest()) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "회원 요청만 거래 상태를 변경할 수 있습니다."
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
                    "매물 작성자 또는 허용된 관리자만 "
                            + "거래 상태를 변경할 수 있습니다."
            );
        }
    }

    private Property saveAndFlush(Property property) {
        try {
            return propertyRepository.saveAndFlush(property);
        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 사용자가 먼저 매물 상태를 변경했습니다."
            );
        }
    }
}