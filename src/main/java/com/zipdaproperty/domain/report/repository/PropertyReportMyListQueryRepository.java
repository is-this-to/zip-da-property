package com.zipdaproperty.domain.report.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.report.entity.QPropertyReport.propertyReport;

@Repository
@RequiredArgsConstructor
public class PropertyReportMyListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PropertyReportMyListQueryRow> findMyReports(
            Long reporterMemberId,
            Instant cursorCreatedAt,
            Long cursorReportId,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PropertyReportMyListQueryRow.class,
                        propertyReport.reportId,
                        propertyReport.propertyId,
                        propertyReport.reasonCode,
                        propertyReport.status,
                        propertyReport.createdAt
                ))
                .from(propertyReport)
                .where(
                        propertyReport.reporterMemberId.eq(reporterMemberId),
                        propertyReport.deletedAt.isNull(),
                        cursorCondition(cursorCreatedAt, cursorReportId)
                )
                .orderBy(
                        propertyReport.createdAt.desc(),
                        propertyReport.reportId.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(Instant cursorCreatedAt, Long cursorReportId) {
        if (cursorCreatedAt == null || cursorReportId == null) {
            return null;
        }

        return propertyReport.createdAt.lt(cursorCreatedAt)
                .or(propertyReport.createdAt.eq(cursorCreatedAt)
                        .and(propertyReport.reportId.lt(cursorReportId)));
    }
}
