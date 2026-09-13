package com.zipdaproperty.domain.property.verification.repository;

public record PropertyVerificationExpirationCandidate(
        Long propertyVerificationId,
        Long propertyId
) {
}
