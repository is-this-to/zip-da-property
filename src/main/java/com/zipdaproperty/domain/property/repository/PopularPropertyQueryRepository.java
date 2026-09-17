package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PopularPropertyRegion;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdaproperty.domain.favorite.entity.QPropertyFavorite.propertyFavorite;
import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.region.entity.QRegion.region;

@Repository
@RequiredArgsConstructor
public class PopularPropertyQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PopularPropertyQueryRow> findPopularProperties(
            PopularPropertyRegion regionFilter,
            int limit
    ) {
        NumberExpression<Long> favoriteCount = propertyFavorite.favoriteId.count();

        return queryFactory
                .select(Projections.constructor(
                        PopularPropertyQueryRow.class,
                        property.propertyId,
                        property.title,
                        property.propertyType,
                        property.transactionType,
                        property.salePrice,
                        property.deposit,
                        property.monthlyRent,
                        property.exclusiveArea,
                        region.fullRegionName,
                        favoriteCount
                ))
                .from(property)
                .join(region)
                .on(
                        region.regionId.eq(property.regionId),
                        region.isActive.isTrue(),
                        region.deletedAt.isNull()
                )
                .leftJoin(propertyFavorite)
                .on(
                        propertyFavorite.propertyId.eq(property.propertyId),
                        propertyFavorite.deletedAt.isNull()
                )
                .where(
                        property.deletedAt.isNull(),
                        property.publicationStatus.eq(PublicationStatus.PUBLISHED),
                        property.transactionStatus.in(
                                TransactionStatus.AVAILABLE,
                                TransactionStatus.RESERVED
                        ),
                        property.verificationStatus.in(
                                VerificationStatus.OWNER_VERIFIED,
                                VerificationStatus.TENANT_VERIFIED,
                                VerificationStatus.AGENT_VERIFIED
                        ),
                        regionCondition(regionFilter)
                )
                .groupBy(
                        property.propertyId,
                        property.title,
                        property.propertyType,
                        property.transactionType,
                        property.salePrice,
                        property.deposit,
                        property.monthlyRent,
                        property.exclusiveArea,
                        region.fullRegionName,
                        property.publishedAt
                )
                .having(favoriteCount.gt(0L))
                .orderBy(
                        favoriteCount.desc(),
                        property.publishedAt.desc(),
                        property.propertyId.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression regionCondition(PopularPropertyRegion regionFilter) {
        String prefix = regionFilter.regionCodePrefix();
        return prefix == null ? null : region.regionCode.startsWith(prefix);
    }
}
