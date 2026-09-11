package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyRevision;
import com.zipdaproperty.domain.property.event.PropertyKafkaEventPublisher;
import com.zipdaproperty.domain.property.event.constant.PropertyEventType;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.repository.PropertyRevisionRepository;
import com.zipdaproperty.domain.property.repository.PropertyStatusHistoryRepository;
import com.zipdaproperty.domain.property.service.PropertyVersionPolicy;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationDecision;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationEvidenceRepository;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationEvidenceRequest;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationReviewRequest;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationSubmitRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyVerificationServiceTest {

    private static final Long PROPERTY_ID = 884685586571263701L;
    private static final Long VERIFICATION_ID = 884700000000000001L;
    private static final Long EVIDENCE_ID = 884700000000000002L;
    private static final Long FILE_ID = 884700000000000003L;
    private static final Long OWNER_ID = 1001L;
    private static final Long ADMIN_ID = 3003L;

    private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
    private final PropertyVerificationRepository verificationRepository = mock(PropertyVerificationRepository.class);
    private final PropertyVerificationEvidenceRepository evidenceRepository = mock(PropertyVerificationEvidenceRepository.class);
    private final PropertyFileRepository propertyFileRepository = mock(PropertyFileRepository.class);
    private final PropertyRevisionRepository revisionRepository = mock(PropertyRevisionRepository.class);
    private final PropertyStatusHistoryRepository statusHistoryRepository = mock(PropertyStatusHistoryRepository.class);
    private final PropertyAuditEventRecorder auditEventRecorder = mock(PropertyAuditEventRecorder.class);
    private final PropertyKafkaEventPublisher kafkaEventPublisher = mock(PropertyKafkaEventPublisher.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final PropertyVerificationService service = new PropertyVerificationService(
            propertyRepository,
            verificationRepository,
            evidenceRepository,
            propertyFileRepository,
            revisionRepository,
            statusHistoryRepository,
            new PropertyVersionPolicy(),
            new PropertyVerificationPolicy(),
            auditEventRecorder,
            kafkaEventPublisher,
            tsidGenerator,
            objectMapper
    );

    @Test
    void submit_validOwnerAndEvidence_recordsVerificationHistoryAuditAndKafka() {
        ActorContext owner = ActorContext.member(OWNER_ID, ActorRole.USER, "verification-submit-test");
        Property property = mock(Property.class);
        PropertyFile file = mock(PropertyFile.class);
        PropertyRevision revision = mock(PropertyRevision.class);

        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(0L, 0L, 1L, 1L, 1L, 1L);
        when(property.getAuthorMemberId()).thenReturn(OWNER_ID);
        when(property.getPublisherType()).thenReturn(PublisherType.DIRECT_OWNER);
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.UNVERIFIED,
                VerificationStatus.UNVERIFIED,
                VerificationStatus.IN_REVIEW,
                VerificationStatus.IN_REVIEW,
                VerificationStatus.IN_REVIEW
        );
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(propertyRepository.saveAndFlush(property)).thenReturn(property);
        when(verificationRepository.existsByPropertyIdAndVerificationTypeAndStatusInAndDeletedAtIsNull(
                eq(PROPERTY_ID), eq(PropertyVerificationType.OWNER), any()
        )).thenReturn(false);
        when(verificationRepository.findTopByPropertyIdAndVerificationTypeOrderByVerificationVersionDesc(
                PROPERTY_ID, PropertyVerificationType.OWNER
        )).thenReturn(Optional.empty());
        when(file.getOwnerMemberId()).thenReturn(OWNER_ID);
        when(file.getFilePurpose()).thenReturn(FilePurpose.VERIFICATION);
        when(file.isVerificationCompleted()).thenReturn(true);
        when(propertyFileRepository.findForVerificationLink(FILE_ID)).thenReturn(Optional.of(file));
        when(tsidGenerator.generate()).thenReturn(VERIFICATION_ID, EVIDENCE_ID);
        when(verificationRepository.saveAndFlush(any(PropertyVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(property)).thenReturn("{\"before\":true}", "{\"after\":true}");
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[\"verificationStatus\"]");
        when(revision.getPropertyRevisionId()).thenReturn(10L);
        when(revisionRepository.saveAndFlush(any(PropertyRevision.class))).thenReturn(revision);

        PropertyVerificationSubmitRequest request = new PropertyVerificationSubmitRequest(
                0L,
                List.of(new PropertyVerificationEvidenceRequest(
                        FILE_ID, PropertyVerificationEvidenceType.REGISTRY_DOCUMENT, 0
                ))
        );

        PropertyVerificationResponse response = service.submit(PROPERTY_ID, request, owner);

        assertThat(response.propertyVerificationId()).isEqualTo(VERIFICATION_ID);
        assertThat(response.verificationType()).isEqualTo(PropertyVerificationType.OWNER);
        assertThat(response.verificationRequestStatus()).isEqualTo(PropertyVerificationStatus.IN_REVIEW);
        verify(property).changeVerificationStatus(VerificationStatus.IN_REVIEW, owner);
        ArgumentCaptor<List<PropertyVerificationEvidence>> evidenceCaptor = ArgumentCaptor.forClass(List.class);
        verify(evidenceRepository).saveAll(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue()).hasSize(1);
        assertThat(evidenceCaptor.getValue().getFirst().getPropertyFileId()).isEqualTo(FILE_ID);
        verify(auditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID), eq(PropertyAuditActionCode.PROPERTY_VERIFICATION_REQUESTED),
                isNull(), isNull(), any(Instant.class), eq(owner)
        );
        verify(kafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID), eq(1L), eq(PropertyEventType.PROPERTY_VERIFICATION_REQUESTED),
                anyMap(), any(Instant.class), eq(owner)
        );
    }

    @Test
    void submit_propertyImageEvidence_throwsEvidenceInvalid() {
        ActorContext owner = ActorContext.member(
                OWNER_ID,
                ActorRole.USER,
                "verification-purpose-test"
        );
        Property property = prepareSubmittableProperty();
        PropertyFile file = mock(PropertyFile.class);

        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(verificationRepository
                .existsByPropertyIdAndVerificationTypeAndStatusInAndDeletedAtIsNull(
                        eq(PROPERTY_ID),
                        eq(PropertyVerificationType.OWNER),
                        any()
                )).thenReturn(false);
        when(propertyFileRepository.findForVerificationLink(FILE_ID))
                .thenReturn(Optional.of(file));
        when(file.getOwnerMemberId()).thenReturn(OWNER_ID);
        when(file.getFilePurpose()).thenReturn(FilePurpose.PROPERTY_IMAGE);

        PropertyVerificationSubmitRequest request = createSubmitRequest();

        assertThatThrownBy(() -> service.submit(PROPERTY_ID, request, owner))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID)
                );

        verify(verificationRepository, never())
                .saveAndFlush(any(PropertyVerification.class));
        verify(propertyRepository, never()).saveAndFlush(any(Property.class));
    }

    @Test
    void submit_uncompletedVerificationEvidence_throwsEvidenceInvalid() {
        ActorContext owner = ActorContext.member(
                OWNER_ID,
                ActorRole.USER,
                "verification-status-test"
        );
        Property property = prepareSubmittableProperty();
        PropertyFile file = mock(PropertyFile.class);

        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(verificationRepository
                .existsByPropertyIdAndVerificationTypeAndStatusInAndDeletedAtIsNull(
                        eq(PROPERTY_ID),
                        eq(PropertyVerificationType.OWNER),
                        any()
                )).thenReturn(false);
        when(propertyFileRepository.findForVerificationLink(FILE_ID))
                .thenReturn(Optional.of(file));
        when(file.getOwnerMemberId()).thenReturn(OWNER_ID);
        when(file.getFilePurpose()).thenReturn(FilePurpose.VERIFICATION);
        when(file.isVerificationCompleted()).thenReturn(false);

        assertThatThrownBy(() -> service.submit(
                PROPERTY_ID,
                createSubmitRequest(),
                owner
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID)
        );

        verify(verificationRepository, never())
                .saveAndFlush(any(PropertyVerification.class));
        verify(propertyRepository, never()).saveAndFlush(any(Property.class));
    }

    @Test
    void review_approve_changesPropertyToOwnerVerifiedAndPublishesEvent() {
        ActorContext owner = ActorContext.member(OWNER_ID, ActorRole.USER, "verification-origin");
        ActorContext admin = ActorContext.member(ADMIN_ID, ActorRole.CS_ADMIN, "verification-review-test");
        Property property = mock(Property.class);
        PropertyRevision revision = mock(PropertyRevision.class);
        PropertyVerification verification = PropertyVerification.submit(
                VERIFICATION_ID, PROPERTY_ID, PropertyVerificationType.OWNER,
                OWNER_ID, 1, Instant.now(), owner
        );

        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(1L, 1L, 2L, 2L, 2L, 2L);
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.IN_REVIEW,
                VerificationStatus.OWNER_VERIFIED,
                VerificationStatus.OWNER_VERIFIED,
                VerificationStatus.OWNER_VERIFIED
        );
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID)).thenReturn(Optional.of(property));
        when(propertyRepository.saveAndFlush(property)).thenReturn(property);
        when(verificationRepository.findByPropertyVerificationIdAndPropertyIdAndDeletedAtIsNull(
                VERIFICATION_ID, PROPERTY_ID
        )).thenReturn(Optional.of(verification));
        when(verificationRepository.saveAndFlush(verification)).thenReturn(verification);
        when(objectMapper.writeValueAsString(property)).thenReturn("{\"before\":true}", "{\"after\":true}");
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[\"verificationStatus\"]");
        when(revision.getPropertyRevisionId()).thenReturn(11L);
        when(revisionRepository.saveAndFlush(any(PropertyRevision.class))).thenReturn(revision);

        PropertyVerificationResponse response = service.review(
                PROPERTY_ID,
                VERIFICATION_ID,
                new PropertyVerificationReviewRequest(1L, PropertyVerificationDecision.APPROVE, "서류 확인 완료"),
                admin
        );

        assertThat(verification.getStatus()).isEqualTo(PropertyVerificationStatus.VERIFIED);
        assertThat(verification.getReviewerMemberId()).isEqualTo(ADMIN_ID);
        assertThat(Duration.between(
                verification.getVerifiedAt(),
                verification.getExpiresAt()
        )).isEqualTo(Duration.ofDays(30));
        assertThat(response.verificationRequestStatus()).isEqualTo(PropertyVerificationStatus.VERIFIED);
        verify(property).changeVerificationStatus(VerificationStatus.OWNER_VERIFIED, admin);
        verify(auditEventRecorder).recordPropertyAction(
                eq(PROPERTY_ID), eq(PropertyAuditActionCode.PROPERTY_VERIFICATION_APPROVED),
                eq("서류 확인 완료"), isNull(), any(Instant.class), eq(admin)
        );
        verify(kafkaEventPublisher).publishAfterCommit(
                eq(PROPERTY_ID), eq(2L), eq(PropertyEventType.PROPERTY_VERIFICATION_APPROVED),
                anyMap(), any(Instant.class), eq(admin)
        );
    }

    @Test
    void review_approveTenantVerification_changesPropertyToTenantVerified() {
        ActorContext tenant = ActorContext.member(
                OWNER_ID,
                ActorRole.USER,
                "tenant-verification-origin"
        );
        ActorContext admin = ActorContext.member(
                ADMIN_ID,
                ActorRole.CS_ADMIN,
                "tenant-verification-review-test"
        );
        Property property = mock(Property.class);
        PropertyRevision revision = mock(PropertyRevision.class);
        PropertyVerification verification = PropertyVerification.submit(
                VERIFICATION_ID,
                PROPERTY_ID,
                PropertyVerificationType.TENANT,
                OWNER_ID,
                1,
                Instant.now(),
                tenant
        );

        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(
                1L,
                1L,
                2L,
                2L,
                2L,
                2L
        );
        when(property.getVerificationStatus()).thenReturn(
                VerificationStatus.IN_REVIEW,
                VerificationStatus.TENANT_VERIFIED,
                VerificationStatus.TENANT_VERIFIED,
                VerificationStatus.TENANT_VERIFIED
        );
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(propertyRepository.saveAndFlush(property))
                .thenReturn(property);
        when(verificationRepository
                .findByPropertyVerificationIdAndPropertyIdAndDeletedAtIsNull(
                        VERIFICATION_ID,
                        PROPERTY_ID
                )).thenReturn(Optional.of(verification));
        when(verificationRepository.saveAndFlush(verification))
                .thenReturn(verification);
        when(objectMapper.writeValueAsString(property))
                .thenReturn("{\"before\":true}", "{\"after\":true}");
        when(objectMapper.writeValueAsString(any(List.class)))
                .thenReturn("[\"verificationStatus\"]");
        when(revision.getPropertyRevisionId()).thenReturn(12L);
        when(revisionRepository.saveAndFlush(any(PropertyRevision.class)))
                .thenReturn(revision);

        PropertyVerificationResponse response = service.review(
                PROPERTY_ID,
                VERIFICATION_ID,
                new PropertyVerificationReviewRequest(
                        1L,
                        PropertyVerificationDecision.APPROVE,
                        "임대차 증빙 확인 완료"
                ),
                admin
        );

        assertThat(response.verificationType())
                .isEqualTo(PropertyVerificationType.TENANT);
        assertThat(response.verificationStatus())
                .isEqualTo(VerificationStatus.TENANT_VERIFIED);
        verify(property).changeVerificationStatus(
                VerificationStatus.TENANT_VERIFIED,
                admin
        );
    }

    private Property prepareSubmittableProperty() {
        Property property = mock(Property.class);
        when(property.getPropertyId()).thenReturn(PROPERTY_ID);
        when(property.getVersion()).thenReturn(0L);
        when(property.getAuthorMemberId()).thenReturn(OWNER_ID);
        when(property.getPublisherType()).thenReturn(PublisherType.DIRECT_OWNER);
        when(property.getVerificationStatus()).thenReturn(VerificationStatus.UNVERIFIED);
        return property;
    }

    private PropertyVerificationSubmitRequest createSubmitRequest() {
        return new PropertyVerificationSubmitRequest(
                0L,
                List.of(new PropertyVerificationEvidenceRequest(
                        FILE_ID,
                        PropertyVerificationEvidenceType.REGISTRY_DOCUMENT,
                        0
                ))
        );
    }
}
