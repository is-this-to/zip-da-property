package com.zipdaproperty.domain.favorite.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.QProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PropertyFavoriteTargetQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsPubliclyAvailable(Long propertyId) {
        QProperty property = QProperty.property;

        return queryFactory
                .selectOne()
                .from(property)
                .where(
                        property.propertyId.eq(propertyId),
                        property.deletedAt.isNull(),
                        property.publicationStatus.eq(
                                PublicationStatus.PUBLISHED
                        ),
                        property.transactionStatus.in(
                                TransactionStatus.AVAILABLE,
                                TransactionStatus.RESERVED
                        ),
                        property.verificationStatus.in(
                                VerificationStatus.OWNER_VERIFIED,
                                VerificationStatus.TENANT_VERIFIED,
                                VerificationStatus.AGENT_VERIFIED
                        )
                )
                .fetchFirst() != null;
    }
}
