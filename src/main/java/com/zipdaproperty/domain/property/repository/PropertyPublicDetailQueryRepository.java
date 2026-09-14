package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.entity.QPropertyAddress.propertyAddress;

@Repository
@RequiredArgsConstructor
public class PropertyPublicDetailQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final PropertyPublicSearchPredicates publicSearchPredicates;

    public Optional<PropertyPublicDetailQueryRow> findPublicDetail(Long propertyId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(
                                PropertyPublicDetailQueryRow.class,
                                property,
                                propertyAddress.publicAddress,
                                propertyAddress.publicLocation
                        ))
                        .from(propertyAddress)
                        .join(propertyAddress.property, property)
                        .where(
                                property.propertyId.eq(propertyId),
                                publicSearchPredicates.publiclyVisibleProperty()
                        )
                        .fetchOne()
        );
    }
}
