package com.zipdaproperty.domain.property.verification.entity;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyVerification extends BaseAuditEntity {

    @Id
    @Column(name = "property_verification_id", nullable = false, updatable = false)
    private Long propertyVerificationId;

    @Column(name = "property_id", nullable = false, updatable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, updatable = false, length = 30)
    private PropertyVerificationType verificationType;

    @Column(name = "applicant_member_id", nullable = false, updatable = false)
    private Long applicantMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PropertyVerificationStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant submittedAt;

    @Column(name = "reviewer_member_id")
    private Long reviewerMemberId;

    @Column(name = "reviewer_role", length = 30)
    private String reviewerRole;

    @Column(name = "reviewed_at", columnDefinition = "DATETIME(6)")
    private Instant reviewedAt;

    @Column(name = "verified_at", columnDefinition = "DATETIME(6)")
    private Instant verifiedAt;

    @Column(name = "expires_at", columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    @Column(name = "result_code", length = 50)
    private String resultCode;

    @Column(name = "result_reason", length = 1000)
    private String resultReason;

    @Column(name = "verification_version", nullable = false, updatable = false)
    private Integer verificationVersion;

    private PropertyVerification(
            Long id,
            Long propertyId,
            PropertyVerificationType type,
            Long applicantMemberId,
            Integer verificationVersion,
            Instant submittedAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyVerificationId = id;
        this.propertyId = propertyId;
        this.verificationType = type;
        this.applicantMemberId = applicantMemberId;
        this.status = PropertyVerificationStatus.IN_REVIEW;
        this.submittedAt = submittedAt;
        this.verificationVersion = verificationVersion;
    }

    public static PropertyVerification submit(
            Long id,
            Long propertyId,
            PropertyVerificationType type,
            Long applicantMemberId,
            Integer verificationVersion,
            Instant submittedAt,
            ActorContext actorContext
    ) {
        return new PropertyVerification(
                id, propertyId, type, applicantMemberId,
                verificationVersion, submittedAt, actorContext
        );
    }

    public void approve(String reason, Instant reviewedAt, Instant expiresAt, ActorContext actorContext) {
        this.status = PropertyVerificationStatus.VERIFIED;
        this.reviewerMemberId = actorContext.memberId();
        this.reviewerRole = actorContext.role().name();
        this.reviewedAt = reviewedAt;
        this.verifiedAt = reviewedAt;
        this.expiresAt = expiresAt;
        this.resultCode = "APPROVED";
        this.resultReason = reason;
        recordUpdate(actorContext);
    }

    public void reject(String reason, Instant reviewedAt, ActorContext actorContext) {
        this.status = PropertyVerificationStatus.REJECTED;
        this.reviewerMemberId = actorContext.memberId();
        this.reviewerRole = actorContext.role().name();
        this.reviewedAt = reviewedAt;
        this.verifiedAt = null;
        this.expiresAt = null;
        this.resultCode = "REJECTED";
        this.resultReason = reason;
        recordUpdate(actorContext);
    }

    public boolean expireIfDue(
            Instant currentTime,
            ActorContext actorContext
    ) {
        if (status != PropertyVerificationStatus.VERIFIED
                || expiresAt == null
                || expiresAt.isAfter(currentTime)) {
            return false;
        }

        this.status = PropertyVerificationStatus.EXPIRED;
        this.resultCode = "EXPIRED";
        this.resultReason = "인증 유효기간이 만료되었습니다.";
        recordUpdate(actorContext);
        return true;
    }
}
