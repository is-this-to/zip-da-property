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
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationExpirationCandidate;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
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

class PropertyVerificationExpirationServiceTest {

    private static final Long PROPERTY_ID = 1001L;
    private static final Long VERIFICATION_ID = 2001L;
    private static final Long REVISION_ID = 3001L;
    private static final Instant CURRENT_TIME =
            Instant.parse("2026-09-11T09:00:00Z");

    private final PropertyVerificationRepository verificationRepository =
            mock(PropertyVerificationRepository.class);
    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);
    private final PropertyRevisionRepository revisionRepository =
            mock(PropertyRevisionRepository.class);
    private final PropertyStatusHistoryRepository statusHistoryRepository =
            mock(PropertyStatusHistoryRepository.class);
    private final PropertyAuditEventRecorder auditEventRecorder =
            mock(PropertyAuditEventRecorder.class);
    private final PropertyKafkaEventPublisher kafkaEventPublisher =
            mock(PropertyKafkaEventPublisher.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final PropertyVerificationExpirationService service =
            new PropertyVerificationExpirationService(
                    verificationRepository,
                    propertyRepository,
                    revisionRepository,
                    statusHistoryRepository,
                    auditEventRecorder,
                    kafkaEventPublisher,
                    objectMapper
            );

    @Test
    void expireDueVerifications_expiresPropertyAndRecordsEvents() {
        PropertyVerification verification = approvedVerification();
        Property property = mock(Property.class);
        PropertyRevision revision = mock(PropertyRevision.class);

        when(verificationRepository.findDueForExpiration(
                eq(PropertyVerificationStatus.VERIFIED),
                eq(CURRENT_TIME),
                eq(PageRequest.of(
                        0,
                        PropertyVerificationExpirationService
                                .EXPIRATION_BATCH_SIZE
                ))
        )).thenReturn(List.of(
                new PropertyVerificationExpirationCandidate(
                        VERIFICATION_ID,
                        PROPERTY_ID
                )
        ));
        when(propertyRepository.findForVerificationExpiration(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(verificationRepository.findForExpiration(VERIFICATION_ID))
                .thenReturn(Optional.of(verification));
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(2L);
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.OWNER_VERIFIED,
                VerificationStatus.EXPIRED
        );
        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);
        when(objectMapper.writeValueAsString(property))
                .thenReturn("{\"before\":true}", "{\"after\":true}");
        when(objectMapper.writeValueAsString(any(List.class)))
                .thenReturn("[\"verificationStatus\"]");
        when(revision.getPropertyRevisionId()).thenReturn(REVISION_ID);
        when(revisionRepository.saveAndFlush(any(PropertyRevision.class)))
                .thenReturn(revision);

        int expiredCount = service.expireDueVerifications(CURRENT_TIME);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(verification.getStatus())
                .isEqualTo(PropertyVerificationStatus.EXPIRED);
        assertThat(verification.getResultCode()).isEqualTo("EXPIRED");
        verify(property).changeVerificationStatus(
                eq(VerificationStatus.EXPIRED),
                any(ActorContext.class)
        );
        verify(verificationRepository).saveAndFlush(verification);

        ArgumentCaptor<PropertyStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(PropertyStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatusType())
                .isEqualTo(PropertyStatusType.VERIFICATION);
        assertThat(historyCaptor.getValue().getBeforeStatus())
                .isEqualTo(VerificationStatus.OWNER_VERIFIED.name());
        assertThat(historyCaptor.getValue().getAfterStatus())
                .isEqualTo(VerificationStatus.EXPIRED.name());

        verify(auditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID),
                eq(PropertyAuditActionCode.PROPERTY_VERIFICATION_EXPIRED),
                eq("인증 유효기간이 만료되었습니다."),
                eq(null),
                eq(CURRENT_TIME),
                any(ActorContext.class)
        );
        verify(kafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID),
                eq(2L),
                eq(PropertyEventType.PROPERTY_VERIFICATION_EXPIRED),
                anyMap(),
                eq(CURRENT_TIME),
                any(ActorContext.class)
        );
    }

    @Test
    void expireDueVerifications_withoutCandidates_doesNothing() {
        when(verificationRepository.findDueForExpiration(
                eq(PropertyVerificationStatus.VERIFIED),
                eq(CURRENT_TIME),
                any()
        )).thenReturn(List.of());

        int expiredCount = service.expireDueVerifications(CURRENT_TIME);

        assertThat(expiredCount).isZero();
        verify(propertyRepository, never())
                .findForVerificationExpiration(any());
        verify(verificationRepository, never())
                .findForExpiration(any());
        verify(kafkaEventPublisher, never())
                .publishAfterCommit(
                        any(), any(), any(), anyMap(), any(), any()
                );
    }

    @Test
    void expireDueVerifications_alreadyExpiredByAnotherRunner_skipsEvents() {
        PropertyVerification verification = approvedVerification();
        verification.expireIfDue(
                CURRENT_TIME,
                ActorContext.batch("other-expiration-runner")
        );
        Property property = mock(Property.class);

        when(verificationRepository.findDueForExpiration(
                eq(PropertyVerificationStatus.VERIFIED),
                eq(CURRENT_TIME),
                any()
        )).thenReturn(List.of(
                new PropertyVerificationExpirationCandidate(
                        VERIFICATION_ID,
                        PROPERTY_ID
                )
        ));
        when(propertyRepository.findForVerificationExpiration(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(verificationRepository.findForExpiration(VERIFICATION_ID))
                .thenReturn(Optional.of(verification));

        int expiredCount = service.expireDueVerifications(CURRENT_TIME);

        assertThat(expiredCount).isZero();
        verify(property, never()).changeVerificationStatus(
                any(),
                any()
        );
        verify(auditEventRecorder, never()).recordPropertyAction(
                any(), any(), any(), any(), any(), any()
        );
        verify(kafkaEventPublisher, never()).publishAfterCommit(
                any(), any(), any(), anyMap(), any(), any()
        );
    }

    private PropertyVerification approvedVerification() {
        ActorContext applicant = ActorContext.member(
                4001L,
                ActorRole.USER,
                "verification-expiration-approval"
        );
        PropertyVerification verification = PropertyVerification.submit(
                VERIFICATION_ID,
                PROPERTY_ID,
                PropertyVerificationType.OWNER,
                4001L,
                1,
                CURRENT_TIME.minusSeconds(31L * 24L * 60L * 60L),
                applicant
        );
        verification.approve(
                "승인",
                CURRENT_TIME.minusSeconds(30L * 24L * 60L * 60L),
                CURRENT_TIME,
                ActorContext.member(
                        5001L,
                        ActorRole.CS_ADMIN,
                        "verification-expiration-review"
                )
        );
        return verification;
    }
}
