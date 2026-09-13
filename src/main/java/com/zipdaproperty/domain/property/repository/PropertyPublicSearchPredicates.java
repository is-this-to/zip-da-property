package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.entity.QPropertyAddress.propertyAddress;

@Component
public class PropertyPublicSearchPredicates {

    public BooleanExpression[] conditions(
            Polygon boundsPolygon,
            PropertyMapSearchCondition condition
    ) {
        return new BooleanExpression[]{
                publiclyVisibleProperty(),
                publicLocationWithin(boundsPolygon),
                propertyTypeCondition(condition),
                transactionAndPriceCondition(condition),
                rangeCondition(property.maintenanceFee, condition.minMaintenanceFee(), condition.maxMaintenanceFee()),
                rangeCondition(property.exclusiveArea, condition.minExclusiveArea(), condition.maxExclusiveArea()),
                rangeCondition(property.roomCount, condition.roomCountMin(), condition.roomCountMax()),
                publisherTypeCondition(condition),
                approvalDateCondition(condition),
                equalIfPresent(property.isParkingAvailable, condition.isParkingAvailable()),
                equalIfPresent(property.hasElevator, condition.hasElevator()),
                equalIfPresent(property.isPetAllowed, condition.isPetAllowed())
        };
    }

    public OrderSpecifier<?>[] orderSpecifiers(PropertyMapSearchCondition condition) {
        return switch (condition.sort()) {
            case LATEST -> new OrderSpecifier<?>[]{
                    property.createdAt.desc(),
                    property.propertyId.desc()
            };
            case PRICE_ASC -> new OrderSpecifier<?>[]{
                    representativePrice().asc().nullsLast(),
                    property.deposit.asc().nullsLast(),
                    property.propertyId.asc()
            };
            case PRICE_DESC -> new OrderSpecifier<?>[]{
                    representativePrice().desc().nullsLast(),
                    property.deposit.desc().nullsLast(),
                    property.propertyId.desc()
            };
            case AREA_DESC -> new OrderSpecifier<?>[]{
                    property.exclusiveArea.desc().nullsLast(),
                    property.propertyId.desc()
            };
        };
    }

    public NumberExpression<Long> representativePrice() {
        return new CaseBuilder()
                .when(property.transactionType.eq(TransactionType.SALE))
                .then(property.salePrice)
                .when(property.transactionType.eq(TransactionType.JEONSE))
                .then(property.deposit)
                .otherwise(property.monthlyRent);
    }

    private BooleanExpression publiclyVisibleProperty() {
        return property.publicationStatus.eq(PublicationStatus.PUBLISHED)
                .and(property.transactionStatus.in(
                        TransactionStatus.AVAILABLE,
                        TransactionStatus.RESERVED
                ))
                .and(property.deletedAt.isNull())
                .and(property.verificationStatus.in(
                        VerificationStatus.OWNER_VERIFIED,
                        VerificationStatus.TENANT_VERIFIED,
                        VerificationStatus.AGENT_VERIFIED
                ))
                .and(propertyAddress.deletedAt.isNull());
    }

    private BooleanExpression publicLocationWithin(Polygon boundsPolygon) {
        return Expressions.booleanTemplate(
                "st_intersects({0}, {1}) = true",
                Expressions.constant(boundsPolygon),
                propertyAddress.publicLocation
        );
    }

    private BooleanExpression propertyTypeCondition(PropertyMapSearchCondition condition) {
        return condition.propertyTypes().isEmpty()
                ? null
                : property.propertyType.in(condition.propertyTypes());
    }

    private BooleanExpression publisherTypeCondition(PropertyMapSearchCondition condition) {
        return condition.publisherTypes().isEmpty()
                ? null
                : property.publisherType.in(condition.publisherTypes());
    }

    private BooleanExpression transactionAndPriceCondition(PropertyMapSearchCondition condition) {
        BooleanExpression result = null;

        if (accepts(condition, TransactionType.SALE)) {
            BooleanExpression sale = property.transactionType.eq(TransactionType.SALE);
            sale = andIfPresent(sale, rangeCondition(
                    property.salePrice,
                    condition.minSalePrice(),
                    condition.maxSalePrice()
            ));
            result = or(result, sale);
        }

        if (accepts(condition, TransactionType.JEONSE)) {
            BooleanExpression jeonse = property.transactionType.eq(TransactionType.JEONSE);
            jeonse = andIfPresent(jeonse, rangeCondition(
                    property.deposit,
                    condition.minDeposit(),
                    condition.maxDeposit()
            ));
            result = or(result, jeonse);
        }

        if (accepts(condition, TransactionType.MONTHLY_RENT)) {
            BooleanExpression monthlyRent = property.transactionType.eq(TransactionType.MONTHLY_RENT);
            monthlyRent = andIfPresent(monthlyRent, rangeCondition(
                    property.deposit,
                    condition.minDeposit(),
                    condition.maxDeposit()
            ));
            monthlyRent = andIfPresent(monthlyRent, rangeCondition(
                    property.monthlyRent,
                    condition.minMonthlyRent(),
                    condition.maxMonthlyRent()
            ));
            result = or(result, monthlyRent);
        }

        return result;
    }

    private BooleanExpression approvalDateCondition(PropertyMapSearchCondition condition) {
        BooleanExpression expression = null;

        if (condition.approvalDateFrom() != null) {
            expression = property.approvalDate.goe(condition.approvalDateFrom());
        }
        if (condition.approvalDateTo() != null) {
            expression = andIfPresent(
                    expression,
                    property.approvalDate.loe(condition.approvalDateTo())
            );
        }
        return expression;
    }

    private BooleanExpression equalIfPresent(
            com.querydsl.core.types.dsl.BooleanPath path,
            Boolean value
    ) {
        return value == null ? null : path.eq(value);
    }

    private boolean accepts(
            PropertyMapSearchCondition condition,
            TransactionType transactionType
    ) {
        return condition.transactionTypes().isEmpty()
                || condition.transactionTypes().contains(transactionType);
    }

    private <N extends Number & Comparable<?>> BooleanExpression rangeCondition(
            NumberPath<N> path,
            N min,
            N max
    ) {
        BooleanExpression expression = min == null ? null : path.goe(min);
        return max == null ? expression : andIfPresent(expression, path.loe(max));
    }

    private BooleanExpression andIfPresent(
            BooleanExpression source,
            BooleanExpression additional
    ) {
        if (source == null) {
            return additional;
        }
        return additional == null ? source : source.and(additional);
    }

    private BooleanExpression or(BooleanExpression left, BooleanExpression right) {
        return left == null ? right : left.or(right);
    }
}
