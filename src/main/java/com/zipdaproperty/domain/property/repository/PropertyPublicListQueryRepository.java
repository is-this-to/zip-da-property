package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.model.PropertyListBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import com.zipdaproperty.domain.property.model.PropertyPublicListCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.entity.QPropertyAddress.propertyAddress;

@Repository
@RequiredArgsConstructor
public class PropertyPublicListQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final PropertyPublicSearchPredicates publicSearchPredicates;

    public List<PropertyPublicListQueryRow> findPublicProperties(
            PropertyListBounds bounds,
            PropertyMapSearchCondition condition,
            PropertyPublicListCursor cursor,
            int limit
    ) {
        NumberExpression<Long> representativePrice =
                publicSearchPredicates.representativePrice();

        return jpaQueryFactory
                .select(Projections.constructor(
                        PropertyPublicListQueryRow.class,
                        property.propertyId,
                        property.regionId,
                        property.title,
                        property.propertyType,
                        property.transactionType,
                        property.salePrice,
                        property.deposit,
                        property.monthlyRent,
                        property.maintenanceFee,
                        property.exclusiveArea,
                        property.roomCount,
                        property.publisherType,
                        propertyAddress.publicAddress,
                        propertyAddress.publicLocation,
                        property.createdAt,
                        representativePrice
                ))
                .from(propertyAddress)
                .join(propertyAddress.property, property)
                .where(publicSearchPredicates.conditions(bounds.toPolygon(), condition))
                .where(cursorCondition(cursor, representativePrice))
                .orderBy(publicSearchPredicates.orderSpecifiers(condition))
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(
            PropertyPublicListCursor cursor,
            NumberExpression<Long> representativePrice
    ) {
        if (cursor == null) {
            return null;
        }

        return switch (cursor.sort()) {
            case LATEST -> property.createdAt.lt(cursor.createdAt())
                    .or(property.createdAt.eq(cursor.createdAt())
                            .and(property.propertyId.lt(cursor.propertyId())));
            case PRICE_ASC -> priceCursorCondition(
                    representativePrice,
                    cursor,
                    true
            );
            case PRICE_DESC -> priceCursorCondition(
                    representativePrice,
                    cursor,
                    false
            );
            case AREA_DESC -> property.exclusiveArea.lt(cursor.exclusiveArea())
                    .or(property.exclusiveArea.eq(cursor.exclusiveArea())
                            .and(property.propertyId.lt(cursor.propertyId())));
        };
    }

    private BooleanExpression priceCursorCondition(
            NumberExpression<Long> representativePrice,
            PropertyPublicListCursor cursor,
            boolean ascending
    ) {
        BooleanExpression idTail = ascending
                ? property.propertyId.gt(cursor.propertyId())
                : property.propertyId.lt(cursor.propertyId());

        BooleanExpression depositTail = nullableNumberTail(
                property.deposit,
                cursor.deposit(),
                idTail,
                ascending
        );

        return nullableNumberTail(
                representativePrice,
                cursor.representativePrice(),
                depositTail,
                ascending
        );
    }

    private BooleanExpression nullableNumberTail(
            NumberExpression<Long> expression,
            Long cursorValue,
            BooleanExpression equalValueTail,
            boolean ascending
    ) {
        if (cursorValue == null) {
            return expression.isNull().and(equalValueTail);
        }

        BooleanExpression laterValue = ascending
                ? expression.gt(cursorValue)
                : expression.lt(cursorValue);

        return laterValue
                .or(expression.isNull())
                .or(expression.eq(cursorValue).and(equalValueTail));
    }
}
