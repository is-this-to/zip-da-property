package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRenewalCandidate;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationRenewalNotificationServiceTest {

    private static final Long PROPERTY_ID = 1001L;
    private static final Long VERIFICATION_ID = 2001L;
    private static final Instant CURRENT_TIME =
            Instant.parse("2026-09-11T09:00:00Z");

    private final PropertyVerificationRepository verificationRepository =
            mock(PropertyVerificationRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyAuditEventRecorder auditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyKafkaEventPublisher kafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);
    private final PropertyVerificationRenewalNotificationService service =
            new PropertyVerificationRenewalNotificationService(
                    verificationRepository,
                    propertyRepository,
                    auditEventRecorder,
                    kafkaEventPublisher
            );

    @Test
    void notifyDueRenewals_marksNotifiedAndPublishesEvents() {
        PropertyVerification verification = approvedVerification();
        Property property = mock(Property.class);
        stubCandidates(verification, property);
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(3L);
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.OWNER_VERIFIED
        );

        int notifiedCount = service.notifyDueRenewals(CURRENT_TIME);

        assertThat(notifiedCount).isEqualTo(1);
        assertThat(verification.getRenewalNotifiedAt())
                .isEqualTo(CURRENT_TIME);
        verify(verificationRepository).saveAndFlush(verification);
        verify(auditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID),
                eq(PropertyAuditActionCode
                        .PROPERTY_VERIFICATION_RENEWAL_NOTIFIED),
                eq("매물 인증 만료 7일 전 재인증 안내를 발송했습니다."),
                eq(null),
                eq(CURRENT_TIME),
                any(ActorContext.class)
        );
        verify(kafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(3L),
                eq(PropertyEventType.PROPERTY_VERIFICATION_RENEWAL_DUE),
                anyMap(),
                eq(CURRENT_TIME),
                any(ActorContext.class)
        );
    }

    @Test
    void notifyDueRenewals_alreadyNotifiedByAnotherRunner_skipsEvents() {
        PropertyVerification verification = approvedVerification();
        verification.markRenewalNotificationIfDue(
                CURRENT_TIME,
                ActorContext.batch("other-renewal-notifier")
        );
        Property property = mock(Property.class);
        stubCandidates(verification, property);
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.OWNER_VERIFIED
        );

        int notifiedCount = service.notifyDueRenewals(CURRENT_TIME);

        assertThat(notifiedCount).isZero();
        verify(verificationRepository, never()).saveAndFlush(any());
        verify(auditEventRecorder, never()).recordPropertyAction(
                any(), any(), any(), any(), any(), any()
        );
        verify(kafkaEventPublisher, never()).publishAfterCommit(
                any(), any(), any(), anyMap(), any(), any()
        );
    }

    @Test
    void notifyDueRenewals_withoutCandidates_doesNothing() {
        when(verificationRepository.findDueForRenewalNotification(
                eq(PropertyVerificationStatus.VERIFIED),
                eq(CURRENT_TIME),
                eq(CURRENT_TIME.plus(7L, ChronoUnit.DAYS)),
                eq(PageRequest.of(
                        0,
                        PropertyVerificationRenewalNotificationService
                                .NOTIFICATION_BATCH_SIZE
                ))
        )).thenReturn(List.of());

        assertThat(service.notifyDueRenewals(CURRENT_TIME)).isZero();
        verify(propertyRepository, never())
                .findForVerificationChange(any());
    }

    private void stubCandidates(
            PropertyVerification verification,
            Property property
    ) {
        when(verificationRepository.findDueForRenewalNotification(
                eq(PropertyVerificationStatus.VERIFIED),
                eq(CURRENT_TIME),
                eq(CURRENT_TIME.plus(7L, ChronoUnit.DAYS)),
                eq(PageRequest.of(
                        0,
                        PropertyVerificationRenewalNotificationService
                                .NOTIFICATION_BATCH_SIZE
                ))
        )).thenReturn(List.of(
                new PropertyVerificationRenewalCandidate(
                        VERIFICATION_ID,
                        PROPERTY_ID
                )
        ));
        when(propertyRepository.findForVerificationChange(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(verificationRepository.findForRenewalNotification(
                VERIFICATION_ID
        )).thenReturn(Optional.of(verification));
    }

    private PropertyVerification approvedVerification() {
        ActorContext owner = ActorContext.member(
                4001L,
                ActorRole.USER,
                "renewal-notification-origin"
        );
        PropertyVerification verification = PropertyVerification.submit(
                VERIFICATION_ID,
                PROPERTY_ID,
                PropertyVerificationType.OWNER,
                4001L,
                1,
                CURRENT_TIME.minus(24L, ChronoUnit.DAYS),
                owner
        );
        verification.approve(
                "승인",
                CURRENT_TIME.minus(24L, ChronoUnit.DAYS),
                CURRENT_TIME.plus(6L, ChronoUnit.DAYS),
                ActorContext.member(
                        5001L,
                        ActorRole.CS_ADMIN,
                        "renewal-notification-review"
                )
        );
        return verification;
    }
}
