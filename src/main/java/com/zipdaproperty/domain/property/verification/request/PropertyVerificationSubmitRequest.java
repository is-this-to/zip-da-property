package com.zipdaproperty.domain.property.verification.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record PropertyVerificationSubmitRequest(
        @NotNull @PositiveOrZero Long version,
        @NotEmpty List<@Valid PropertyVerificationEvidenceRequest> evidence
) {
    public PropertyVerificationSubmitRequest {
        evidence = evidence == null ? null : List.copyOf(evidence);
    }
}
