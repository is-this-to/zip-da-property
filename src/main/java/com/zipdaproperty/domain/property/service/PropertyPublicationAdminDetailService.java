package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.audit.constant.PropertyAuditActionCode;
import com.zipdaproperty.domain.property.audit.service.PropertyAuditEventRecorder;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminDetailResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminVerificationSummary;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PropertyPublicationAdminDetailService {

  private final PropertyEditDetailService editDetailService;
  private final PropertyRepository propertyRepository;
  private final PropertyVerificationRepository verificationRepository;
  private final PropertyAuditEventRecorder auditRecorder;
  private final PropertyAdminReviewAccess access;

  @Transactional
  public PropertyPublicationAdminDetailResponse find(
      Long propertyId,
      String auditReason,
      ActorContext actor
  ) {
    access.requireAdmin(actor);
    // String reason = access.requireReason(auditReason);
    String reason = auditReason;

    Property property = propertyRepository
        .findByPropertyIdAndDeletedAtIsNull(propertyId)
        .orElseThrow(() -> new BusinessException(
            CustomResponseCode.PROPERTY_NOT_FOUND,
            "매물을 찾을 수 없습니다."
        ));

    PropertyEditDetailResponse detail =
        editDetailService.getEditDetail(propertyId, actor);

    PropertyPublicationAdminVerificationSummary latest =
        verificationRepository
            .findTopByPropertyIdAndDeletedAtIsNullOrderBySubmittedAtDescPropertyVerificationIdDesc(
                propertyId
            )
            .map(this::summary)
            .orElse(null);

    auditRecorder.recordAction(
        "PROPERTY_PUBLICATION_REVIEW",
        propertyId.toString(),
        PropertyAuditActionCode.PROPERTY_PUBLICATION_REVIEW_DETAIL_VIEWED,
        reason,
        null,
        Instant.now(),
        actor
    );

    return new PropertyPublicationAdminDetailResponse(
        property.getAuthorMemberId(),
        detail,
        latest
    );
  }

  private PropertyPublicationAdminVerificationSummary summary(
      PropertyVerification verification
  ) {
    return new PropertyPublicationAdminVerificationSummary(
        verification.getPropertyVerificationId(),
        verification.getVerificationType(),
        verification.getStatus(),
        verification.getSubmittedAt(),
        verification.getReviewedAt(),
        verification.getVerifiedAt(),
        verification.getExpiresAt(),
        verification.getResultReason()
    );
  }
}
