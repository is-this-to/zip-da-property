package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.constant.FilePurpose;
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
import com.zipdaproperty.domain.property.service.PropertyVersionPolicy;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationDecision;
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
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyVerificationService {

    private static final String VERIFICATION_STATUS_FIELD = "verificationStatus";
    private static final long VERIFICATION_VALID_DAYS = 30L;

    private final PropertyRepository propertyRepository;
    private final PropertyVerificationRepository verificationRepository;
    private final PropertyVerificationEvidenceRepository evidenceRepository;
    private final PropertyFileRepository propertyFileRepository;
    private final PropertyRevisionRepository propertyRevisionRepository;
    private final PropertyStatusHistoryRepository propertyStatusHistoryRepository;
    private final PropertyVersionPolicy propertyVersionPolicy;
    private final PropertyVerificationPolicy verificationPolicy;
    private final PropertyAuditEventRecorder auditEventRecorder;
    private final PropertyKafkaEventPublisher kafkaEventPublisher;
    private final TsidGenerator tsidGenerator;
    private final ObjectMapper objectMapper;

    @Transactional
    public PropertyVerificationResponse submit(
            Long propertyId,
            PropertyVerificationSubmitRequest request,
            ActorContext actorContext
    ) {
        Property property = findProperty(propertyId);
        propertyVersionPolicy.validate(property.getVersion(), request.version());
        PropertyVerificationType type = verificationPolicy.validateSubmission(property, actorContext);

        boolean activeExists = verificationRepository
                .existsByPropertyIdAndVerificationTypeAndStatusInAndDeletedAtIsNull(
                        propertyId,
                        type,
                        List.of(PropertyVerificationStatus.IN_REVIEW)
                );
        if (activeExists) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    "처리 중인 동일 유형의 검증 신청이 이미 존재합니다."
            );
        }

        validateEvidence(request.evidence(), actorContext);
        Instant occurredAt = Instant.now();
        String beforeSnapshotJson = objectMapper.writeValueAsString(property);
        VerificationStatus beforeStatus = property.getVerificationStatus();

        PropertyVerification verification = PropertyVerification.submit(
                tsidGenerator.generate(),
                propertyId,
                type,
                actorContext.memberId(),
                nextVerificationVersion(propertyId, type),
                occurredAt,
                actorContext
        );
        PropertyVerification savedVerification = verificationRepository.saveAndFlush(verification);
        saveEvidence(savedVerification.getPropertyVerificationId(), request.evidence(), actorContext);

        property.changeVerificationStatus(VerificationStatus.IN_REVIEW, actorContext);
        Property savedProperty = saveProperty(property);
        recordStatusChange(savedProperty, beforeStatus, null, actorContext, occurredAt, beforeSnapshotJson);
        recordEvents(savedProperty, savedVerification, PropertyEventType.PROPERTY_VERIFICATION_REQUESTED,
                PropertyAuditActionCode.PROPERTY_VERIFICATION_REQUESTED, null, occurredAt, actorContext);

        return PropertyVerificationResponse.from(savedVerification, savedProperty);
    }

    @Transactional
    public PropertyVerificationResponse review(
            Long propertyId,
            Long verificationId,
            PropertyVerificationReviewRequest request,
            ActorContext actorContext
    ) {
        Property property = findProperty(propertyId);
        propertyVersionPolicy.validate(property.getVersion(), request.version());
        PropertyVerification verification = verificationRepository
                .findByPropertyVerificationIdAndPropertyIdAndDeletedAtIsNull(verificationId, propertyId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PROPERTY_VERIFICATION_NOT_FOUND,
                        "매물 검증 신청을 찾을 수 없습니다."
                ));
        verificationPolicy.validateReview(verification, actorContext);
        validateReviewReason(request);

        Instant occurredAt = Instant.now();
        String beforeSnapshotJson = objectMapper.writeValueAsString(property);
        VerificationStatus beforeStatus = property.getVerificationStatus();
        boolean approved = request.decision() == PropertyVerificationDecision.APPROVE;

        if (approved) {
            verification.approve(
                    request.reason(), occurredAt,
                    occurredAt.plus(VERIFICATION_VALID_DAYS, ChronoUnit.DAYS), actorContext
            );
            VerificationStatus approvedStatus = switch (
                    verification.getVerificationType()
            ) {
                case OWNER -> VerificationStatus.OWNER_VERIFIED;
                case TENANT -> VerificationStatus.TENANT_VERIFIED;
                case AGENT_BROKERAGE -> VerificationStatus.AGENT_VERIFIED;
            };
            property.changeVerificationStatus(approvedStatus, actorContext);
        } else {
            verification.reject(request.reason(), occurredAt, actorContext);
            property.changeVerificationStatus(VerificationStatus.REJECTED, actorContext);
        }

        PropertyVerification savedVerification = verificationRepository.saveAndFlush(verification);
        Property savedProperty = saveProperty(property);
        recordStatusChange(savedProperty, beforeStatus, request.reason(), actorContext, occurredAt, beforeSnapshotJson);

        String eventType = approved
                ? PropertyEventType.PROPERTY_VERIFICATION_APPROVED
                : PropertyEventType.PROPERTY_VERIFICATION_REJECTED;
        String actionCode = approved
                ? PropertyAuditActionCode.PROPERTY_VERIFICATION_APPROVED
                : PropertyAuditActionCode.PROPERTY_VERIFICATION_REJECTED;
        recordEvents(savedProperty, savedVerification, eventType, actionCode,
                request.reason(), occurredAt, actorContext);

        return PropertyVerificationResponse.from(savedVerification, savedProperty);
    }

    private Property findProperty(Long propertyId) {
        if (propertyId == null || propertyId <= 0) {
            throw new BusinessException(CustomResponseCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
        return propertyRepository.findByPropertyIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다. propertyId = " + propertyId
                ));
    }

    private void validateEvidence(
            List<PropertyVerificationEvidenceRequest> evidenceRequests,
            ActorContext actorContext
    ) {
        Set<Long> fileIds = new HashSet<>();
        List<PropertyVerificationEvidenceRequest> lockOrderedRequests =
                evidenceRequests.stream()
                        .sorted(Comparator.comparing(
                                PropertyVerificationEvidenceRequest::propertyFileId
                        ))
                        .toList();
        for (PropertyVerificationEvidenceRequest evidenceRequest
                : lockOrderedRequests) {
            if (!fileIds.add(evidenceRequest.propertyFileId())) {
                throw invalidEvidence("같은 증빙 파일을 중복으로 제출할 수 없습니다.");
            }
            PropertyFile file = propertyFileRepository
                    .findForVerificationLink(evidenceRequest.propertyFileId())
                    .orElseThrow(() -> invalidEvidence("증빙 파일을 찾을 수 없습니다."));
            if (!Objects.equals(file.getOwnerMemberId(), actorContext.memberId())) {
                throw new BusinessException(
                        CustomResponseCode.FILE_OWNERSHIP_REQUIRED,
                        "본인이 업로드한 증빙 파일만 제출할 수 있습니다."
                );
            }
            if (file.getFilePurpose() != FilePurpose.VERIFICATION) {
                throw invalidEvidence("검증 용도로 업로드한 파일만 증빙으로 제출할 수 있습니다.");
            }
            if (!file.isVerificationCompleted()) {
                throw invalidEvidence("업로드 완료 처리된 파일만 증빙으로 제출할 수 있습니다.");
            }
        }
    }

    private BusinessException invalidEvidence(String message) {
        return new BusinessException(CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID, message);
    }

    private int nextVerificationVersion(Long propertyId, PropertyVerificationType type) {
        return verificationRepository
                .findTopByPropertyIdAndVerificationTypeOrderByVerificationVersionDesc(propertyId, type)
                .map(item -> item.getVerificationVersion() + 1)
                .orElse(1);
    }

    private void saveEvidence(
            Long verificationId,
            List<PropertyVerificationEvidenceRequest> requests,
            ActorContext actorContext
    ) {
        List<PropertyVerificationEvidence> evidence = requests.stream()
                .map(request -> new PropertyVerificationEvidence(
                        tsidGenerator.generate(), verificationId, request.propertyFileId(),
                        request.evidenceType(), request.sortOrder(), actorContext
                ))
                .toList();
        evidenceRepository.saveAll(evidence);
    }

    private void validateReviewReason(PropertyVerificationReviewRequest request) {
        if (request.decision() == PropertyVerificationDecision.REJECT
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "검증 반려 사유는 필수입니다."
            );
        }
    }

    private Property saveProperty(Property property) {
        try {
            return propertyRepository.saveAndFlush(property);
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(
                    CustomResponseCode.VERSION_CONFLICT,
                    "다른 사용자가 먼저 매물 검증 상태를 변경했습니다."
            );
        }
    }

    private void recordStatusChange(
            Property property,
            VerificationStatus beforeStatus,
            String reason,
            ActorContext actorContext,
            Instant occurredAt,
            String beforeSnapshotJson
    ) {
        String afterSnapshotJson = objectMapper.writeValueAsString(property);
        String changedFieldsJson = objectMapper.writeValueAsString(List.of(VERIFICATION_STATUS_FIELD));
        PropertyRevision revision = PropertyRevision.statusChanged(
                property.getPropertyId(), property.getVersion(), changedFieldsJson,
                beforeSnapshotJson, afterSnapshotJson, reason, actorContext, occurredAt
        );
        PropertyRevision savedRevision = propertyRevisionRepository.saveAndFlush(revision);
        propertyStatusHistoryRepository.save(new PropertyStatusHistory(
                property.getPropertyId(), savedRevision.getPropertyRevisionId(),
                PropertyStatusType.VERIFICATION, beforeStatus.name(),
                property.getVerificationStatus().name(), null, reason,
                property.getVersion(), occurredAt, actorContext
        ));
    }

    private void recordEvents(
            Property property,
            PropertyVerification verification,
            String eventType,
            String actionCode,
            String reason,
            Instant occurredAt,
            ActorContext actorContext
    ) {
        auditEventRecorder.recordPropertyAction(
                property.getPropertyId(), actionCode, reason, null, occurredAt, actorContext
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propertyId", property.getPropertyId().toString());
        payload.put("propertyVerificationId", verification.getPropertyVerificationId().toString());
        payload.put("version", property.getVersion());
        payload.put("verificationType", verification.getVerificationType().name());
        payload.put("verificationRequestStatus", verification.getStatus().name());
        payload.put("verificationStatus", property.getVerificationStatus().name());
        payload.put("reason", reason);
        kafkaEventPublisher.publishAfterCommit(
                property.getPropertyId(), property.getVersion(), eventType,
                payload, occurredAt, actorContext
        );
    }
}
