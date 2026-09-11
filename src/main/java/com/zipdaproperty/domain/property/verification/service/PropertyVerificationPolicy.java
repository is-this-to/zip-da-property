package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PropertyVerificationPolicy {

    public PropertyVerificationType validateSubmission(
            Property property,
            ActorContext actorContext
    ) {
        if (actorContext == null || !actorContext.isMemberRequest()
                || !Objects.equals(property.getAuthorMemberId(), actorContext.memberId())) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    "매물 작성자만 검증을 신청할 수 있습니다."
            );
        }

        PropertyVerificationType type;
        if (property.getPublisherType() == PublisherType.DIRECT_OWNER
                && actorContext.role() == ActorRole.USER) {
            type = PropertyVerificationType.OWNER;
        } else if (property.getPublisherType() == PublisherType.DIRECT_TENANT
                && actorContext.role() == ActorRole.USER) {
            type = PropertyVerificationType.TENANT;
        } else if (property.getPublisherType() == PublisherType.AGENT_BROKERAGE
                && actorContext.role() == ActorRole.AGENT) {
            type = PropertyVerificationType.AGENT_BROKERAGE;
        } else {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "등록 유형과 회원 역할에 맞는 검증만 신청할 수 있습니다."
            );
        }

        VerificationStatus status = property.getVerificationStatus();
        if (status != VerificationStatus.UNVERIFIED
                && status != VerificationStatus.REJECTED
                && status != VerificationStatus.EXPIRED
                && !isVerified(status)) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    "현재 상태에서는 새 검증을 신청할 수 없습니다."
            );
        }
        return type;
    }

    public void validateRenewalWindow(
            PropertyVerification latestVerification,
            PropertyVerificationType expectedType,
            Instant currentTime
    ) {
        boolean renewable = latestVerification != null
                && latestVerification.getVerificationType() == expectedType
                && latestVerification.getStatus()
                == PropertyVerificationStatus.VERIFIED
                && latestVerification.getExpiresAt() != null
                && latestVerification.getExpiresAt().isAfter(currentTime)
                && !latestVerification.getExpiresAt()
                .minus(7, ChronoUnit.DAYS)
                .isAfter(currentTime);
        if (!renewable) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    "인증 만료 7일 전부터 재인증을 신청할 수 있습니다."
            );
        }
    }

    public boolean isVerified(VerificationStatus status) {
        return status == VerificationStatus.OWNER_VERIFIED
                || status == VerificationStatus.TENANT_VERIFIED
                || status == VerificationStatus.AGENT_VERIFIED;
    }

    public void validateReview(PropertyVerification verification, ActorContext actorContext) {
        if (actorContext == null || !actorContext.isMemberRequest()
                || (actorContext.role() != ActorRole.CS_ADMIN
                && actorContext.role() != ActorRole.SUPER_ADMIN)) {
            throw new BusinessException(
                    CustomResponseCode.FORBIDDEN,
                    "검증 담당 관리자만 검토할 수 있습니다."
            );
        }
        if (verification.getStatus() != PropertyVerificationStatus.IN_REVIEW) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_VERIFICATION_REVIEW_NOT_ALLOWED,
                    "검토 대기 중인 신청만 승인하거나 반려할 수 있습니다."
            );
        }
    }
}
