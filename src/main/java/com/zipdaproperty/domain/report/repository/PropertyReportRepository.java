package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface PropertyReportRepository
        extends JpaRepository<PropertyReport, Long> {

    boolean existsByReporterMemberIdAndPropertyIdAndReasonCodeAndStatusInAndDeletedAtIsNull(
            Long reporterMemberId,
            Long propertyId,
            ReportReasonCode reasonCode,
            Collection<ReportStatus> statuses
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM property_report
                    WHERE reporter_member_id = :reporterMemberId
                      AND created_at >= CURRENT_DATE
                      AND created_at < CURRENT_DATE + INTERVAL 1 DAY
                    """,
            nativeQuery = true
    )
    long countDailyReportsIncludingDeleted(
            @Param("reporterMemberId") Long reporterMemberId
    );
}
