package com.zipdaproperty.domain.favorite.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.favorite.entity.QPropertyFavorite.propertyFavorite;
import static com.zipdaproperty.domain.property.entity.QProperty.property;

@Repository
@RequiredArgsConstructor
public class PropertyFavoriteListQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<PropertyFavoriteListQueryRow> findMyFavorites(
            Long memberId,
            Instant lastFavoritedAt,
            Long lastPropertyId,
            int limit
    ) {
        return queryFactory
                .select(
                        Projections.constructor(
                                PropertyFavoriteListQueryRow.class,
                                propertyFavorite.propertyId,
                                propertyFavorite.createdAt
                        )
                )
                .from(propertyFavorite)
                .join(property)
                .on(
                        property.propertyId.eq(
                                propertyFavorite.propertyId
                        )
                )
                .where(
                        propertyFavorite.memberId.eq(memberId),
                        propertyFavorite.deletedAt.isNull(),
                        property.deletedAt.isNull(),
                        property.publicationStatus.eq(
                                PublicationStatus.PUBLISHED
                        ),
                        property.transactionStatus.in(
                                TransactionStatus.AVAILABLE,
                                TransactionStatus.RESERVED
                        ),
                        cursorCondition(
                                lastFavoritedAt,
                                lastPropertyId
                        )
                )
                .orderBy(
                        propertyFavorite.createdAt.desc(),
                        propertyFavorite.propertyId.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(
            Instant lastFavoritedAt,
            Long lastPropertyId
    ) {
        if (lastFavoritedAt == null && lastPropertyId == null) {
            return null;
        }

        if (lastFavoritedAt == null || lastPropertyId == null) {
            throw new IllegalArgumentException(
                    "cursor 기준값은 함께 전달되어야 합니다."
            );
        }

        return propertyFavorite.createdAt.lt(lastFavoritedAt)
                .or(
                        propertyFavorite.createdAt.eq(lastFavoritedAt)
                                .and(
                                        propertyFavorite.propertyId.lt(
                                                lastPropertyId
                                        )
                                )
                );
    }
}
