package com.zipdaproperty.domain.property.verification.entity;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
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
@Table(name = "property_verification_evidence")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyVerificationEvidence extends BaseAuditEntity {

    @Id
    @Column(name = "verification_evidence_id", nullable = false, updatable = false)
    private Long verificationEvidenceId;

    @Column(name = "property_verification_id", nullable = false, updatable = false)
    private Long propertyVerificationId;

    @Column(name = "property_file_id", nullable = false, updatable = false)
    private Long propertyFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, updatable = false, length = 50)
    private PropertyVerificationEvidenceType evidenceType;

    @Column(name = "sort_order", nullable = false, updatable = false)
    private Integer sortOrder;

    public PropertyVerificationEvidence(
            Long id,
            Long propertyVerificationId,
            Long propertyFileId,
            PropertyVerificationEvidenceType evidenceType,
            Integer sortOrder,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.verificationEvidenceId = id;
        this.propertyVerificationId = propertyVerificationId;
        this.propertyFileId = propertyFileId;
        this.evidenceType = evidenceType;
        this.sortOrder = sortOrder;
    }

    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        if (isDeleted()) {
            return;
        }
        recordDeletion(actorContext, deletedAt, deleteReason);
    }
}
