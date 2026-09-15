package com.zipdaproperty.domain.favorite.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.favorite.entity.QPropertyFavorite;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.zipdaproperty.domain.favorite.entity.QPropertyFavorite.propertyFavorite;
import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.region.entity.QRegion.region;

@Repository
@RequiredArgsConstructor
public class PropertyFavoriteListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PropertyFavoriteListQueryRow> findMyFavorites(
            Long memberId,
            long offset,
            int limit
    ) {
        QPropertyFavorite countedFavorite =
                new QPropertyFavorite("countedFavorite");
        return queryFactory
                .select(
                        Projections.constructor(
                                PropertyFavoriteListQueryRow.class,
                                propertyFavorite.propertyId,
                                property.propertyType,
                                property.transactionType,
                                property.salePrice,
                                property.deposit,
                                property.monthlyRent,
                                property.exclusiveArea,
                                region.fullRegionName,
                                property.floor,
                                property.publisherType,
                                JPAExpressions
                                        .select(countedFavorite.count())
                                        .from(countedFavorite)
                                        .where(
                                                countedFavorite.propertyId.eq(
                                                        property.propertyId
                                                )
                                        )
                        )
                )
                .from(propertyFavorite)
                .join(property)
                .on(
                        property.propertyId.eq(
                                propertyFavorite.propertyId
                        )
                )
                .join(region)
                .on(
                        region.regionId.eq(property.regionId),
                        region.isActive.isTrue()
                )
                .where(
                        propertyFavorite.memberId.eq(memberId),
                        property.deletedAt.isNull(),
                        property.publicationStatus.eq(
                                PublicationStatus.PUBLISHED
                        ),
                        property.transactionStatus.in(
                                TransactionStatus.AVAILABLE,
                                TransactionStatus.RESERVED
                        ),
                        property.verificationStatus.in(
                                VerificationStatus.OWNER_VERIFIED,
                                VerificationStatus.TENANT_VERIFIED,
                                VerificationStatus.AGENT_VERIFIED
                        )
                )
                .orderBy(
                        propertyFavorite.createdAt.desc(),
                        propertyFavorite.propertyId.desc()
                )
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public Map<Long, Long> countByPropertyIds(
            Collection<Long> propertyIds
    ) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Map.of();
        }

        NumberExpression<Long> favoriteCount = propertyFavorite.count();
        List<Tuple> rows = queryFactory
                .select(
                        propertyFavorite.propertyId,
                        favoriteCount
                )
                .from(propertyFavorite)
                .where(
                        propertyFavorite.propertyId.in(propertyIds),
                        propertyFavorite.deletedAt.isNull()
                )
                .groupBy(propertyFavorite.propertyId)
                .fetch();

        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Tuple row : rows) {
            counts.put(
                    row.get(propertyFavorite.propertyId),
                    row.get(favoriteCount)
            );
        }
        return Map.copyOf(counts);
    }

    public Set<Long> findFavoritePropertyIds(
            Long memberId,
            Collection<Long> propertyIds
    ) {
        if (memberId == null || propertyIds == null || propertyIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(new LinkedHashSet<>(queryFactory
                .select(propertyFavorite.propertyId)
                .distinct()
                .from(propertyFavorite)
                .where(
                        propertyFavorite.memberId.eq(memberId),
                        propertyFavorite.propertyId.in(propertyIds),
                        propertyFavorite.deletedAt.isNull()
                )
                .fetch()));
    }
}
