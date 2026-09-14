package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;

public interface PropertyReportActionRepository
        extends Repository<PropertyReportAction, Long> {

    PropertyReportAction save(PropertyReportAction action);

    Optional<PropertyReportAction>
    findFirstByReportIdAndActionCodeInOrderByExecutedAtDescActionIdDesc(
            Long reportId,
            Collection<ReportActionCode> actionCodes
    );
}
