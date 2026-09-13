package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import org.springframework.data.repository.Repository;

public interface PropertyReportActionRepository
        extends Repository<PropertyReportAction, Long> {

    PropertyReportAction save(PropertyReportAction action);
}
