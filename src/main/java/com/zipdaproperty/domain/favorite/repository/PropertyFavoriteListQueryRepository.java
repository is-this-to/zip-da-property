package com.zipdaproperty.domain.favorite.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.favorite.entity.QPropertyFavorite;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
