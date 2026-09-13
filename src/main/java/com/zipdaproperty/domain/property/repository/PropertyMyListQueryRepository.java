package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;

@Repository
@RequiredArgsConstructor
public class PropertyMyListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PropertyMyListQueryRow> findMyProperties(
            Long memberId,
            Instant cursorUpdatedAt,
            Long cursorPropertyId,
            int limit
    ) {
        return queryFactory
                .select(
                        Projections.constructor(
                                PropertyMyListQueryRow.class,
                                property.propertyId,
                                property.version,
                                property.regionId,
                                property.title,
                                property.propertyType,
                                property.transactionType,
                                property.salePrice,
                                property.deposit,
                                property.monthlyRent,
                                property.publisherType,
                                property.publicationStatus,
                                property.transactionStatus,
                                property.verificationStatus,
                                property.updatedAt
                        )
                )
                .from(property)
                .where(
                        property.authorMemberId.eq(memberId),
                        property.deletedAt.isNull(),
                        cursorCondition(
                                cursorUpdatedAt,
                                cursorPropertyId
                        )
                )
                .orderBy(
                        property.updatedAt.desc(),
                        property.propertyId.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(
            Instant cursorUpdatedAt,
            Long cursorPropertyId
    ) {
        if (cursorUpdatedAt == null
                || cursorPropertyId == null) {
            return null;
        }

        return property.updatedAt
                .lt(cursorUpdatedAt)
                .or(
                        property.updatedAt
                                .eq(cursorUpdatedAt)
                                .and(
                                        property.propertyId
                                                .lt(cursorPropertyId)
                                )
                );
    }
}