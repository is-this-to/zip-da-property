package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.entity.PropertyAddress;
import com.zipdaproperty.domain.property.repository.PropertyAddressRepository;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditAddressResponse;
import com.zipdaproperty.domain.property.service.PropertyAdminReviewAccess;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationEvidenceRepository;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminDetailResponse;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminEvidenceResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyVerificationAdminDetailService {

  private final PropertyVerificationRepository verificationRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyAddressRepository propertyAddressRepository;
  private final PropertyVerificationEvidenceRepository evidenceRepository;
  private final PropertyFileRepository fileRepository;
  private final MinioPresignedGetUrlGenerator getUrlGenerator;
  private final PropertyAuditEventRecorder auditRecorder;
  private final PropertyAdminReviewAccess access;

  @Transactional
  public PropertyVerificationAdminDetailResponse find(
      Long id,
      String auditReason,
      ActorContext actor
  ) {
    access.requireAdmin(actor);
    String reason = access.requireReason(auditReason);

    PropertyVerification verification = verificationRepository
        .findByPropertyVerificationIdAndDeletedAtIsNull(id)
        .orElseThrow(this::notFound);

    Property property = propertyRepository
        .findByPropertyIdAndDeletedAtIsNull(verification.getPropertyId())
        .orElseThrow(this::notFound);

    PropertyAddress address = propertyAddressRepository
        .findByProperty_PropertyIdAndDeletedAtIsNull(property.getPropertyId())
        .orElseThrow(this::notFound);

    List<PropertyVerificationAdminEvidenceResponse> evidence =
        findEvidence(verification);

    auditRecorder.recordAction(
        "PROPERTY_VERIFICATION",
        id.toString(),
        PropertyAuditActionCode.PROPERTY_VERIFICATION_DETAIL_VIEWED,
        reason,
        null,
        Instant.now(),
        actor
    );

    return new PropertyVerificationAdminDetailResponse(
        property.getPropertyId(),
        verification.getPropertyVerificationId(),
        property.getVersion(),
        verification.getVerificationVersion(),

        property.getTitle(),
        property.getPublisherType(),
        property.getPropertyType(),
        property.getTransactionType(),
        property.getPublicationStatus(),
        property.getVerificationStatus(),

        verification.getVerificationType(),
        verification.getStatus(),

        verification.getApplicantMemberId(),
        verification.getReviewerMemberId(),
        verification.getReviewerRole(),

        verification.getSubmittedAt(),
        verification.getReviewedAt(),
        verification.getVerifiedAt(),
        verification.getExpiresAt(),

        verification.getResultCode(),
        verification.getResultReason(),

        PropertyEditAddressResponse.from(address),
        evidence
    );
  }

  private List<PropertyVerificationAdminEvidenceResponse> findEvidence(
      PropertyVerification verification
  ) {
    List<PropertyVerificationEvidence> evidence = evidenceRepository
        .findAllByPropertyVerificationIdAndDeletedAtIsNullOrderBySortOrderAscVerificationEvidenceIdAsc(
            verification.getPropertyVerificationId()
        );

    if (evidence.isEmpty()) {
      return List.of();
    }

    Map<Long, PropertyFile> files = fileRepository
        .findAllByPropertyFileIdInAndDeletedAtIsNull(
            evidence.stream()
                .map(PropertyVerificationEvidence::getPropertyFileId)
                .toList()
        )
        .stream()
        .collect(Collectors.toMap(
            PropertyFile::getPropertyFileId,
            Function.identity()
        ));

    return evidence.stream()
        .filter(item ->
            available(
                files.get(item.getPropertyFileId()),
                verification
            )
        )
        .map(item -> {
          PropertyFile file = files.get(item.getPropertyFileId());

          return new PropertyVerificationAdminEvidenceResponse(
              item.getVerificationEvidenceId(),
              item.getPropertyFileId(),
              item.getEvidenceType(),
              item.getSortOrder(),
              getUrlGenerator.generate(file.getObjectKey())
          );
        })
        .toList();
  }

  private boolean available(
      PropertyFile file,
      PropertyVerification verification
  ) {
    return file != null
        && file.getUploadStatus() == UploadStatus.LINKED
        && file.getFilePurpose() == FilePurpose.VERIFICATION
        && file.getObjectDeletedAt() == null
        && Objects.equals(
        file.getOwnerMemberId(),
        verification.getApplicantMemberId()
    );
  }

  private BusinessException notFound() {
    return new BusinessException(
        CustomResponseCode.NOT_FOUND_RESOURCE,
        "조회할 수 있는 검증 신청이 없습니다."
    );
  }
}