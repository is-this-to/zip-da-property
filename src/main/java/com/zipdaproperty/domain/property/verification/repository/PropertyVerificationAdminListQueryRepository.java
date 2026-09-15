package com.zipdaproperty.domain.property.verification.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.verification.entity.QPropertyVerification.propertyVerification;

@Repository
@RequiredArgsConstructor
public class PropertyVerificationAdminListQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<PropertyVerificationAdminListItemResponse> find(
            PropertyVerificationStatus status, PropertyVerificationType type,
            Instant cursorTime, Long cursorId, int limit
    ) {
        return queryFactory.select(Projections.constructor(
                        PropertyVerificationAdminListItemResponse.class,
                        propertyVerification.propertyId,
                        propertyVerification.propertyVerificationId,
                        property.version,
                        propertyVerification.verificationVersion,
                        propertyVerification.verificationType,
                        propertyVerification.status,
                        property.verificationStatus,
                        propertyVerification.submittedAt))
                .from(propertyVerification)
                .join(property).on(property.propertyId.eq(propertyVerification.propertyId))
                .where(propertyVerification.deletedAt.isNull(), property.deletedAt.isNull(),
                        propertyVerification.status.eq(status),
                        type == null ? null : propertyVerification.verificationType.eq(type),
                        cursorCondition(cursorTime, cursorId))
                .orderBy(propertyVerification.submittedAt.asc(),
                        propertyVerification.propertyVerificationId.asc())
                .limit(limit).fetch();
    }

    private BooleanExpression cursorCondition(Instant time, Long id) {
        if (time == null) return null;
        return propertyVerification.submittedAt.gt(time)
                .or(propertyVerification.submittedAt.eq(time)
                        .and(propertyVerification.propertyVerificationId.gt(id)));
    }
}
