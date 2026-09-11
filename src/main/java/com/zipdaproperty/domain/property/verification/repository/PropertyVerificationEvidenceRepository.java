package com.zipdaproperty.domain.property.verification.repository;

import com.zipdaproperty.domain.property.verification.entity.PropertyVerificationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyVerificationEvidenceRepository
        extends JpaRepository<PropertyVerificationEvidence, Long> {
}
