package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.entity.QPropertyAddress.propertyAddress;

@Repository
@RequiredArgsConstructor
public class PropertyMapBoundsQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<PropertyMapBoundsQueryRow>
    findPublicPropertiesInBounds(
            PropertyMapBounds bounds,
            PropertyMapSearchCondition condition,
            int limit
    ) {
        Polygon boundsPolygon =
                bounds.toPolygon();

        return jpaQueryFactory
                .select(
                        Projections.constructor(
                                PropertyMapBoundsQueryRow.class,
                                property.propertyId,
                                property.regionId,
                                property.title,
                                property.propertyType,
                                property.transactionType,
                                property.salePrice,
                                property.deposit,
                                property.monthlyRent,
                                propertyAddress.publicAddress,
                                propertyAddress.publicLocation
                        )
                )
                .from(propertyAddress)
                .join(
                        propertyAddress.property,
                        property
                )
                .where(
                        conditions(
                                boundsPolygon,
                                condition
                        )
                )
                .orderBy(
                        orderSpecifiers(condition)
                )
                .limit(limit)
                .fetch();
    }

    public long countPublicPropertiesInBounds(
            PropertyMapBounds bounds,
            PropertyMapSearchCondition condition
    ) {
        Polygon boundsPolygon =
                bounds.toPolygon();

        Long result = jpaQueryFactory
                .select(
                        property.propertyId.count()
                )
                .from(propertyAddress)
                .join(
                        propertyAddress.property,
                        property
                )
                .where(
                        conditions(
                                boundsPolygon,
                                condition
                        )
                )
                .fetchOne();

        return result == null
                ? 0L
                : result;
    }

    private BooleanExpression[] conditions(
            Polygon boundsPolygon,
            PropertyMapSearchCondition condition
    ) {
        return new BooleanExpression[]{
                publiclyVisibleProperty(),
                publicLocationWithin(boundsPolygon),
                propertyTypeCondition(condition),
                transactionAndPriceCondition(condition),
                maintenanceFeeCondition(condition),
                exclusiveAreaCondition(condition),
                roomCountCondition(condition),
                publisherTypeCondition(condition),
                approvalDateCondition(condition),
                parkingCondition(condition),
                elevatorCondition(condition),
                petCondition(condition)
        };
    }

    private BooleanExpression propertyTypeCondition(
            PropertyMapSearchCondition condition
    ) {
        if (condition.propertyTypes().isEmpty()) {
            return null;
        }

        return property.propertyType.in(
                condition.propertyTypes()
        );
    }

    private BooleanExpression publisherTypeCondition(
            PropertyMapSearchCondition condition
    ) {
        if (condition.publisherTypes().isEmpty()) {
            return null;
        }

        return property.publisherType.in(
                condition.publisherTypes()
        );
    }

    private BooleanExpression transactionAndPriceCondition(
            PropertyMapSearchCondition condition
    ) {
        BooleanExpression result = null;

        if (accepts(
                condition,
                TransactionType.SALE
        )) {
            BooleanExpression sale =
                    property.transactionType.eq(
                            TransactionType.SALE
                    );

            sale = andIfPresent(
                    sale,
                    rangeCondition(
                            property.salePrice,
                            condition.minSalePrice(),
                            condition.maxSalePrice()
                    )
            );
            result = or(result, sale);
        }

        if (accepts(
                condition,
                TransactionType.JEONSE
        )) {
            BooleanExpression jeonse =
                    property.transactionType.eq(
                            TransactionType.JEONSE
                    );

            jeonse = andIfPresent(
                    jeonse,
                    rangeCondition(
                            property.deposit,
                            condition.minDeposit(),
                            condition.maxDeposit()
                    )
            );
            result = or(result, jeonse);
        }

        if (accepts(
                condition,
                TransactionType.MONTHLY_RENT
        )) {
            BooleanExpression monthlyRent =
                    property.transactionType.eq(
                            TransactionType.MONTHLY_RENT
                    );

            monthlyRent = andIfPresent(
                    monthlyRent,
                    rangeCondition(
                            property.deposit,
                            condition.minDeposit(),
                            condition.maxDeposit()
                    )
            );
            monthlyRent = andIfPresent(
                    monthlyRent,
                    rangeCondition(
                            property.monthlyRent,
                            condition.minMonthlyRent(),
                            condition.maxMonthlyRent()
                    )
            );
            result = or(result, monthlyRent);
        }

        return result;
    }

    private BooleanExpression maintenanceFeeCondition(
            PropertyMapSearchCondition condition
    ) {
        return rangeCondition(
                property.maintenanceFee,
                condition.minMaintenanceFee(),
                condition.maxMaintenanceFee()
        );
    }

    private BooleanExpression exclusiveAreaCondition(
            PropertyMapSearchCondition condition
    ) {
        return rangeCondition(
                property.exclusiveArea,
                condition.minExclusiveArea(),
                condition.maxExclusiveArea()
        );
    }

    private BooleanExpression roomCountCondition(
            PropertyMapSearchCondition condition
    ) {
        return rangeCondition(
                property.roomCount,
                condition.roomCountMin(),
                condition.roomCountMax()
        );
    }

    private BooleanExpression approvalDateCondition(
            PropertyMapSearchCondition condition
    ) {
        BooleanExpression expression = null;

        if (condition.approvalDateFrom() != null) {
            expression = property.approvalDate.goe(
                    condition.approvalDateFrom()
            );
        }

        if (condition.approvalDateTo() != null) {
            BooleanExpression upperBound =
                    property.approvalDate.loe(
                            condition.approvalDateTo()
                    );

            expression = andIfPresent(
                    expression,
                    upperBound
            );
        }

        return expression;
    }

    private BooleanExpression parkingCondition(
            PropertyMapSearchCondition condition
    ) {
        if (condition.isParkingAvailable() == null) {
            return null;
        }

        return property.isParkingAvailable.eq(
                condition.isParkingAvailable()
        );
    }

    private BooleanExpression elevatorCondition(
            PropertyMapSearchCondition condition
    ) {
        if (condition.hasElevator() == null) {
            return null;
        }

        return property.hasElevator.eq(
                condition.hasElevator()
        );
    }

    private BooleanExpression petCondition(
            PropertyMapSearchCondition condition
    ) {
        if (condition.isPetAllowed() == null) {
            return null;
        }

        return property.isPetAllowed.eq(
                condition.isPetAllowed()
        );
    }

    private boolean accepts(
            PropertyMapSearchCondition condition,
            TransactionType transactionType
    ) {
        return condition.transactionTypes().isEmpty()
                || condition.transactionTypes().contains(
                        transactionType
                );
    }

    private <N extends Number & Comparable<?>>
    BooleanExpression rangeCondition(
            NumberPath<N> path,
            N min,
            N max
    ) {
        BooleanExpression expression = null;

        if (min != null) {
            expression = path.goe(min);
        }

        if (max != null) {
            expression = andIfPresent(
                    expression,
                    path.loe(max)
            );
        }

        return expression;
    }

    private BooleanExpression andIfPresent(
            BooleanExpression source,
            BooleanExpression additional
    ) {
        if (source == null) {
            return additional;
        }

        return additional == null
                ? source
                : source.and(additional);
    }

    private BooleanExpression or(
            BooleanExpression left,
            BooleanExpression right
    ) {
        return left == null
                ? right
                : left.or(right);
    }

    private OrderSpecifier<?>[] orderSpecifiers(
            PropertyMapSearchCondition condition
    ) {
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

    private NumberExpression<Long> representativePrice() {
        return new CaseBuilder()
                .when(
                        property.transactionType.eq(
                                TransactionType.SALE
                        )
                )
                .then(property.salePrice)
                .when(
                        property.transactionType.eq(
                                TransactionType.JEONSE
                        )
                )
                .then(property.deposit)
                .otherwise(property.monthlyRent);
    }

    /**
     * 공개 중이고 거래 가능한, 소프트 삭제되지 않은 매물과 주소만 조회한다.
     */
    private BooleanExpression publiclyVisibleProperty() {
        return property.publicationStatus
                .eq(PublicationStatus.PUBLISHED)
                .and(
                        property.transactionStatus.in(
                                TransactionStatus.AVAILABLE,
                                TransactionStatus.RESERVED
                        )
                )
                .and(
                        property.deletedAt.isNull()
                )
                .and(
                        property.verificationStatus.in(
                                VerificationStatus.OWNER_VERIFIED,
                                VerificationStatus.TENANT_VERIFIED,
                                VerificationStatus.AGENT_VERIFIED
                        )
                )
                .and(
                        propertyAddress.deletedAt.isNull()
                );
    }

    /**
     * 공간 인덱스가 설정된 public_location만 지도 bounds 검색에 사용한다.
     * ST_Intersects를 사용하므로 bounds 경계선 위의 Point도 포함한다.
     */
    private BooleanExpression publicLocationWithin(
            Polygon boundsPolygon
    ) {
        return Expressions.booleanTemplate(
                "st_intersects({0}, {1}) = true",
                Expressions.constant(boundsPolygon),
                propertyAddress.publicLocation
        );
    }
}
