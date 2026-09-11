package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRenewalCandidate;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PropertyVerificationRenewalNotificationService {

    static final int NOTIFICATION_BATCH_SIZE = 100;
    static final long RENEWAL_NOTICE_DAYS = 7L;
    private static final String NOTICE_REASON =
            "매물 인증 만료 7일 전 재인증 안내를 발송했습니다.";

    private final PropertyVerificationRepository verificationRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyAuditEventRecorder auditEventRecorder;
    private final PropertyKafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public int notifyDueRenewals(Instant currentTime) {
        Instant requiredCurrentTime = Objects.requireNonNull(
                currentTime,
                "재인증 안내 기준 시각은 필수입니다."
        );
        Instant notifyBefore = requiredCurrentTime.plus(
                RENEWAL_NOTICE_DAYS,
                ChronoUnit.DAYS
        );
        List<PropertyVerificationRenewalCandidate> candidates =
                verificationRepository.findDueForRenewalNotification(
                        PropertyVerificationStatus.VERIFIED,
                        requiredCurrentTime,
                        notifyBefore,
                        PageRequest.of(0, NOTIFICATION_BATCH_SIZE)
                );

        ActorContext actorContext = ActorContext.batch(
                "property-verification-renewal-notification-"
                        + requiredCurrentTime.toEpochMilli()
        );
        int notifiedCount = 0;
        for (PropertyVerificationRenewalCandidate candidate : candidates) {
            Property property = propertyRepository
                    .findForVerificationChange(candidate.propertyId())
                    .orElse(null);
            if (property == null || !isVerified(property)) {
                continue;
            }

            PropertyVerification verification = verificationRepository
                    .findForRenewalNotification(
                            candidate.propertyVerificationId()
                    )
                    .orElse(null);
            if (verification == null
                    || !verification.markRenewalNotificationIfDue(
                    requiredCurrentTime,
                    actorContext
            )) {
                continue;
            }

            verificationRepository.saveAndFlush(verification);
            recordEvents(
                    property,
                    verification,
                    requiredCurrentTime,
                    actorContext
            );
            notifiedCount++;
        }
        return notifiedCount;
    }

    private boolean isVerified(Property property) {
        VerificationStatus status = property.getVerificationStatus();
        return status == VerificationStatus.OWNER_VERIFIED
                || status == VerificationStatus.TENANT_VERIFIED
                || status == VerificationStatus.AGENT_VERIFIED;
    }

    private void recordEvents(
            Property property,
            PropertyVerification verification,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        auditEventRecorder.recordPropertyAction(
                property.getPropertyId(),
                PropertyAuditActionCode
                        .PROPERTY_VERIFICATION_RENEWAL_NOTIFIED,
                NOTICE_REASON,
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
        payload.put(
                "applicantMemberId",
                verification.getApplicantMemberId().toString()
        );
        payload.put(
                "verificationType",
                verification.getVerificationType().name()
        );
        payload.put("expiresAt", verification.getExpiresAt().toString());

        kafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(),
                property.getVersion(),
                PropertyEventType.PROPERTY_VERIFICATION_RENEWAL_DUE,
                payload,
                occurredAt,
                actorContext
        );
    }
}
