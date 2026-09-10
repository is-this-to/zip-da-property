package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
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
                        publiclyVisibleProperty(),
                        publicLocationWithin(boundsPolygon)
                )
                .orderBy(
                        property.propertyId.asc()
                )
                .limit(limit)
                .fetch();
    }

    public long countPublicPropertiesInBounds(
            PropertyMapBounds bounds
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
                        publiclyVisibleProperty(),
                        publicLocationWithin(boundsPolygon)
                )
                .fetchOne();

        return result == null
                ? 0L
                : result;
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
