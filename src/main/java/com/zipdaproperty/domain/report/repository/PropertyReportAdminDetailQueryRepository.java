package com.zipdaproperty.domain.report.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.zipdaproperty.domain.report.entity.QPropertyReport.propertyReport;

@Repository
@RequiredArgsConstructor
public class PropertyReportAdminDetailQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<PropertyReportAdminDetailQueryRow> findReport(Long reportId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        PropertyReportAdminDetailQueryRow.class,
                        propertyReport.reportId,
                        propertyReport.propertyId,
                        propertyReport.reporterMemberId,
                        propertyReport.reasonCode,
                        propertyReport.detail,
                        propertyReport.status,
                        propertyReport.riskScore,
                        propertyReport.assignedAdminId,
                        propertyReport.version,
                        propertyReport.createdAt
                ))
                .from(propertyReport)
                .where(
                        propertyReport.reportId.eq(reportId),
                        propertyReport.deletedAt.isNull()
                )
                .fetchOne());
    }
}
