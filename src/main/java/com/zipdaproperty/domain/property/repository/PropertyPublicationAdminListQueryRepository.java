package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;

@Repository
@RequiredArgsConstructor
public class PropertyPublicationAdminListQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<PropertyPublicationAdminListItemResponse> find(
            PublicationStatus status, VerificationStatus verificationStatus,
            PublisherType publisherType, PropertyType propertyType,
            Instant cursorTime, Long cursorId, int limit
    ) {
        return queryFactory.select(Projections.constructor(PropertyPublicationAdminListItemResponse.class,
                        property.propertyId, property.version, property.publicationStatus,
                        property.verificationStatus, property.publisherType, property.propertyType,
                        property.createdAt))
                .from(property)
                .where(property.deletedAt.isNull(), property.publicationStatus.eq(status),
                        verificationStatus == null ? null : property.verificationStatus.eq(verificationStatus),
                        publisherType == null ? null : property.publisherType.eq(publisherType),
                        propertyType == null ? null : property.propertyType.eq(propertyType),
                        cursorCondition(cursorTime, cursorId))
                .orderBy(property.createdAt.asc(), property.propertyId.asc())
                .limit(limit).fetch();
    }

    private BooleanExpression cursorCondition(Instant time, Long id) {
        if (time == null) return null;
        return property.createdAt.gt(time)
                .or(property.createdAt.eq(time).and(property.propertyId.gt(id)));
    }
}
