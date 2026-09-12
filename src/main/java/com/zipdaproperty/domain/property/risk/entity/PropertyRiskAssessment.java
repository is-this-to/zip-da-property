package com.zipdaproperty.domain.property.risk.entity;

import com.zipdaproperty.domain.property.risk.constant.PropertyRiskDecision;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "property_risk_assessment",
        indexes = {
                @Index(
                        name = "idx_property_risk_assessment_property",
                        columnList = "property_id, evaluated_at"
                ),
                @Index(
                        name = "idx_property_risk_assessment_publisher",
                        columnList = "publisher_member_id, evaluated_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyRiskAssessment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_assessment_id", nullable = false, updatable = false)
    private Long riskAssessmentId;

    @Column(name = "property_id", nullable = false, updatable = false)
    private Long propertyId;

    @Column(name = "publisher_member_id", nullable = false, updatable = false)
    private Long publisherMemberId;

    @Column(
            name = "normalized_address_hash",
            nullable = false,
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String normalizedAddressHash;

    @Column(name = "matched_rule_codes_json", nullable = false, updatable = false, columnDefinition = "JSON")
    private String matchedRuleCodesJson;

    @Column(name = "risk_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, updatable = false, length = 30)
    private PropertyRiskDecision decision;

    @Column(name = "duplicate_property_id", updatable = false)
    private Long duplicatePropertyId;

    @Column(name = "evaluated_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant evaluatedAt;

    private PropertyRiskAssessment(
            Long propertyId,
            Long publisherMemberId,
            String normalizedAddressHash,
            String matchedRuleCodesJson,
            BigDecimal riskScore,
            PropertyRiskDecision decision,
            Long duplicatePropertyId,
            Instant evaluatedAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyId = requirePositive(propertyId, "propertyId");
        this.publisherMemberId = requirePositive(
                publisherMemberId,
                "publisherMemberId"
        );
        this.normalizedAddressHash = requireHash(normalizedAddressHash);
        this.matchedRuleCodesJson = requireText(
                matchedRuleCodesJson,
                "matchedRuleCodesJson"
        );
        this.riskScore = requireScore(riskScore);
        this.decision = Objects.requireNonNull(decision, "decision은 필수입니다.");
        this.duplicatePropertyId = duplicatePropertyId;
        this.evaluatedAt = Objects.requireNonNull(
                evaluatedAt,
                "evaluatedAt은 필수입니다."
        );
    }

    public static PropertyRiskAssessment create(
            Long propertyId,
            Long publisherMemberId,
            String normalizedAddressHash,
            String matchedRuleCodesJson,
            BigDecimal riskScore,
            PropertyRiskDecision decision,
            Long duplicatePropertyId,
            Instant evaluatedAt,
            ActorContext actorContext
    ) {
        return new PropertyRiskAssessment(
                propertyId,
                publisherMemberId,
                normalizedAddressHash,
                matchedRuleCodesJson,
                riskScore,
                decision,
                duplicatePropertyId,
                evaluatedAt,
                actorContext
        );
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 0보다 커야 합니다.");
        }
        return value;
    }

    private static String requireHash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "normalizedAddressHash는 소문자 SHA-256 형식이어야 합니다."
            );
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        return value;
    }

    private static BigDecimal requireScore(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(
                    "riskScore는 0 이상 100 이하여야 합니다."
            );
        }
        return value;
    }
}
