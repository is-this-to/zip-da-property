package com.zipdaproperty.domain.property.risk.model;

import com.zipdaproperty.domain.property.risk.constant.PropertyRiskDecision;
import com.zipdaproperty.domain.property.risk.constant.PropertyRiskRuleCode;

import java.math.BigDecimal;
import java.util.List;

public record PropertyRiskAssessmentResult(
        BigDecimal score,
        PropertyRiskDecision decision,
        List<PropertyRiskRuleCode> matchedRuleCodes,
        Long duplicatePropertyId
) {
    public PropertyRiskAssessmentResult {
        matchedRuleCodes = List.copyOf(matchedRuleCodes);
    }

    public boolean blocked() {
        return decision == PropertyRiskDecision.BLOCKED;
    }

    public boolean exactDuplicateDetected() {
        return matchedRuleCodes.contains(
                PropertyRiskRuleCode.SAME_ADDRESS_IMAGE
        );
    }
}
