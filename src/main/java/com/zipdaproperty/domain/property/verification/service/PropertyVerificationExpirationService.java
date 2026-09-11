package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PropertyStatusType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.entity.PropertyStatusHistory;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationExpirationCandidate;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
public class PropertyVerificationExpirationService {

    static final int EXPIRATION_BATCH_SIZE = 100;
    private static final String VERIFICATION_STATUS_FIELD =
            "verificationStatus";
    private static final String EXPIRATION_REASON =
            "인증 유효기간이 만료되었습니다.";

    private final PropertyVerificationRepository verificationRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyRevisionRepository revisionRepository;
    private final PropertyStatusHistoryRepository statusHistoryRepository;
    private final PropertyAuditEventRecorder auditEventRecorder;
    private final PropertyKafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public int expireDueVerifications(Instant currentTime) {
        Instant requiredCurrentTime = Objects.requireNonNull(
                currentTime,
                "인증 만료 기준 시각은 필수입니다."
        );
        ActorContext actorContext = ActorContext.batch(
                "property-verification-expiration-"
                        + requiredCurrentTime.toEpochMilli()
        );
        List<PropertyVerificationExpirationCandidate> candidates =
                verificationRepository.findDueForExpiration(
                        PropertyVerificationStatus.VERIFIED,
                        requiredCurrentTime,
                        PageRequest.of(0, EXPIRATION_BATCH_SIZE)
                );

        int expiredCount = 0;
        for (PropertyVerificationExpirationCandidate candidate : candidates) {
            Property property = propertyRepository
                    .findForVerificationExpiration(
                            candidate.propertyId()
                    )
                    .orElse(null);

            if (property == null) {
                continue;
            }

            PropertyVerification verification = verificationRepository
                    .findForExpiration(
                            candidate.propertyVerificationId()
                    )
                    .orElse(null);
            if (verification == null
                    || !verification.expireIfDue(
                    requiredCurrentTime,
                    actorContext
            )) {
                continue;
            }

            expireProperty(
                    property,
                    verification,
                    requiredCurrentTime,
                    actorContext
            );
            expiredCount++;
        }

        return expiredCount;
    }

    private void expireProperty(
            Property property,
            PropertyVerification verification,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        VerificationStatus beforeStatus =
                property.getVerificationStatus();
        String beforeSnapshotJson =
                objectMapper.writeValueAsString(property);

        property.changeVerificationStatus(
                VerificationStatus.EXPIRED,
                actorContext
        );
        verificationRepository.saveAndFlush(verification);
        Property savedProperty =
                propertyRepository.saveAndFlush(property);

        recordStatusChange(
                savedProperty,
                beforeStatus,
                occurredAt,
                actorContext,
                beforeSnapshotJson
        );
        recordEvents(
                savedProperty,
                verification,
                occurredAt,
                actorContext
        );
    }

    private void recordStatusChange(
            Property property,
            VerificationStatus beforeStatus,
            Instant occurredAt,
            ActorContext actorContext,
            String beforeSnapshotJson
    ) {
        String afterSnapshotJson =
                objectMapper.writeValueAsString(property);
        String changedFieldsJson =
                objectMapper.writeValueAsString(
                        List.of(VERIFICATION_STATUS_FIELD)
                );
        PropertyRevision revision =
                PropertyRevision.statusChanged(
                        property.getPropertyId(),
                        property.getVersion(),
                        changedFieldsJson,
                        beforeSnapshotJson,
                        afterSnapshotJson,
                        EXPIRATION_REASON,
                        actorContext,
                        occurredAt
                );
        PropertyRevision savedRevision =
                revisionRepository.saveAndFlush(revision);

        statusHistoryRepository.save(
                new PropertyStatusHistory(
                        property.getPropertyId(),
                        savedRevision.getPropertyRevisionId(),
                        PropertyStatusType.VERIFICATION,
                        beforeStatus.name(),
                        VerificationStatus.EXPIRED.name(),
                        "VERIFICATION_EXPIRED",
                        EXPIRATION_REASON,
                        property.getVersion(),
                        occurredAt,
                        actorContext
                )
        );
    }

    private void recordEvents(
            Property property,
            PropertyVerification verification,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        auditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode.PROPERTY_VERIFICATION_EXPIRED,
                EXPIRATION_REASON,
                null,
                occurredAt,
                actorContext
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propertyId", property.getPropertyId().toString());
        payload.put(
                "propertyVerificationId",
                verification.getPropertyVerificationId().toString()
        );
        payload.put("version", property.getVersion());
        payload.put(
                "verificationType",
                verification.getVerificationType().name()
        );
        payload.put(
                "verificationRequestStatus",
                verification.getStatus().name()
        );
        payload.put(
                "verificationStatus",
                property.getVerificationStatus().name()
        );

        kafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                PropertyEventType.PROPERTY_VERIFICATION_EXPIRED,
                payload,
                occurredAt,
                actorContext
        );
    }
}
