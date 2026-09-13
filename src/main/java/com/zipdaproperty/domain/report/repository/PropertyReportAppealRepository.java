package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyReportAppealRepository
        extends JpaRepository<PropertyReportAppeal, Long> {

    boolean existsByReportId(Long reportId);

    Optional<PropertyReportAppeal> findByAppealIdAndDeletedAtIsNull(
            Long appealId
    );
}
