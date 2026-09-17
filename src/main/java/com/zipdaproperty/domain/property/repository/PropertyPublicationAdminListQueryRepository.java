package com.zipdaproperty.domain.property.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static com.zipdaproperty.domain.property.entity.QProperty.property;
import static com.zipdaproperty.domain.property.entity.QPropertyAddress.propertyAddress;

@Repository
@RequiredArgsConstructor
public class PropertyPublicationAdminListQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Page<PropertyPublicationAdminListItemResponse> find(
        PublicationStatus status, VerificationStatus verificationStatus,
        PublisherType publisherType, PropertyType propertyType,
        String address, Instant registeredFrom, Instant registeredToExclusive,
        Pageable pageable
    ) {
        List<PropertyPublicationAdminListItemResponse> content = queryFactory
            .select(Projections.constructor(PropertyPublicationAdminListItemResponse.class,
                property.propertyId, property.version, propertyAddress.publicAddress,
                property.transactionType, property.salePrice, property.deposit,
                property.monthlyRent, property.authorMemberId, property.publicationStatus,
                property.verificationStatus, property.publisherType, property.propertyType,
                property.createdAt))
            .from(property)
            .join(propertyAddress).on(propertyAddress.property.eq(property))
            .where(property.deletedAt.isNull(), propertyAddress.deletedAt.isNull(),
                property.publicationStatus.eq(status),
                verificationStatus == null ? null : property.verificationStatus.eq(verificationStatus),
                publisherType == null ? null : property.publisherType.eq(publisherType),
                propertyType == null ? null : property.propertyType.eq(propertyType),
                address == null || address.isBlank() ? null : propertyAddress.publicAddress.containsIgnoreCase(address),
                registeredFrom == null ? null : property.createdAt.goe(registeredFrom),
                registeredToExclusive == null ? null : property.createdAt.lt(registeredToExclusive))
            .orderBy(property.createdAt.asc(), property.propertyId.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(property.count())
            .from(property)
            .join(propertyAddress).on(propertyAddress.property.eq(property))
            .where(
                property.deletedAt.isNull(), propertyAddress.deletedAt.isNull(),
                property.publicationStatus.eq(status),
                verificationStatus == null ? null : property.verificationStatus.eq(verificationStatus),
                publisherType == null ? null : property.publisherType.eq(publisherType),
                propertyType == null ? null : property.propertyType.eq(propertyType),
                address == null || address.isBlank() ? null : propertyAddress.publicAddress.containsIgnoreCase(address),
                registeredFrom == null ? null : property.createdAt.goe(registeredFrom),
                registeredToExclusive == null ? null : property.createdAt.lt(registeredToExclusive))
            .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
