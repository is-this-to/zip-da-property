package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyReportAppealRepository
        extends JpaRepository<PropertyReportAppeal, Long> {
}
