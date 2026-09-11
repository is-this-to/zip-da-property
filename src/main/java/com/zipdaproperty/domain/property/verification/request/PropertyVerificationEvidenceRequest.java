package com.zipdaproperty.domain.property.verification.request;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PropertyVerificationEvidenceRequest(
        @NotNull @Positive Long propertyFileId,
        @NotNull PropertyVerificationEvidenceType evidenceType,
        @NotNull @PositiveOrZero Integer sortOrder
) {
}
