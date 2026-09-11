package com.zipdaproperty.domain.property.verification.request;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PropertyVerificationReviewRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull PropertyVerificationDecision decision,
        @Size(max = 1000) String reason
) {
}
