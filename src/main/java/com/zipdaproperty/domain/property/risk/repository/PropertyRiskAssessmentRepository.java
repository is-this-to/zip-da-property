package com.zipdaproperty.domain.property.risk.repository;

import com.zipdaproperty.domain.property.risk.entity.PropertyRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRiskAssessmentRepository
        extends JpaRepository<PropertyRiskAssessment, Long> {
}
