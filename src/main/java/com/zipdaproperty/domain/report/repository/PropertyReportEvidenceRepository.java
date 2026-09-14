package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyReportEvidenceRepository
        extends JpaRepository<PropertyReportEvidence, Long> {
}
