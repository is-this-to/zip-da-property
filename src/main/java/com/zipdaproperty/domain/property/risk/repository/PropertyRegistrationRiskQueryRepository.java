package com.zipdaproperty.domain.property.risk.repository;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PropertyRegistrationRiskQueryRepository {

    private static final int MAX_CANDIDATE_COUNT = 100;

    private final EntityManager entityManager;

    public Set<Long> findActivePropertyIdsByAddress(
            String roadAddress,
            String jibunAddress
    ) {
        AddressCondition addressCondition =
                addressCondition(roadAddress, jibunAddress);

        TypedQuery<Long> query = entityManager.createQuery(
                """
                        select property.propertyId
                        from Property property, PropertyAddress address
                        where address.property = property
                          and property.deletedAt is null
                          and address.deletedAt is null
                          and %s
                        order by property.propertyId asc
                        """.formatted(addressCondition.expression()),
                Long.class
        );

        query.setParameter("addressValue", addressCondition.value());
        query.setMaxResults(MAX_CANDIDATE_COUNT);
        return new LinkedHashSet<>(query.getResultList());
    }

    public Set<Long> findActivePropertyIdsByAddressPricePublisher(
            String roadAddress,
            String jibunAddress,
            PropertyCreateCommand command,
            Long publisherMemberId
    ) {
        AddressCondition addressCondition =
                addressCondition(roadAddress, jibunAddress);

        StringBuilder jpql = new StringBuilder("""
                select property.propertyId
                from Property property, PropertyAddress address
                where address.property = property
                  and property.deletedAt is null
                  and address.deletedAt is null
                  and property.authorMemberId = :publisherMemberId
                  and %s
                """.formatted(addressCondition.expression()));

        appendNullablePriceCondition(
                jpql,
                "salePrice",
                command.salePrice()
        );
        appendNullablePriceCondition(
                jpql,
                "deposit",
                command.deposit()
        );
        appendNullablePriceCondition(
                jpql,
                "monthlyRent",
                command.monthlyRent()
        );
        jpql.append(" order by property.propertyId asc");

        TypedQuery<Long> query = entityManager.createQuery(
                jpql.toString(),
                Long.class
        );
        query.setParameter("addressValue", addressCondition.value());
        query.setParameter("publisherMemberId", publisherMemberId);
        setNullablePriceParameter(query, "salePrice", command.salePrice());
        setNullablePriceParameter(query, "deposit", command.deposit());
        setNullablePriceParameter(
                query,
                "monthlyRent",
                command.monthlyRent()
        );
        query.setMaxResults(MAX_CANDIDATE_COUNT);
        return new LinkedHashSet<>(query.getResultList());
    }

    public Set<Long> findActivePropertyIdsByImageChecksums(
            Collection<String> imageChecksums
    ) {
        if (imageChecksums == null || imageChecksums.isEmpty()) {
            return Set.of();
        }

        TypedQuery<Long> query = entityManager.createQuery(
                """
                        select distinct property.propertyId
                        from Property property,
                             PropertyImage image,
                             PropertyFile file
                        where image.propertyId = property.propertyId
                          and image.propertyFileId = file.propertyFileId
                          and property.deletedAt is null
                          and image.deletedAt is null
                          and file.deletedAt is null
                          and file.checksum in :imageChecksums
                        order by property.propertyId asc
                        """,
                Long.class
        );
        query.setParameter("imageChecksums", imageChecksums);
        query.setMaxResults(MAX_CANDIDATE_COUNT);
        return new LinkedHashSet<>(query.getResultList());
    }

    private AddressCondition addressCondition(
            String roadAddress,
            String jibunAddress
    ) {
        if (roadAddress != null) {
            return new AddressCondition(
                    "address.exactRoadAddress = :addressValue",
                    roadAddress
            );
        }
        if (jibunAddress != null) {
            return new AddressCondition(
                    "address.exactJibunAddress = :addressValue",
                    jibunAddress
            );
        }
        throw new IllegalArgumentException(
                "중복 검사에는 도로명 주소 또는 지번 주소가 필요합니다."
        );
    }

    private void appendNullablePriceCondition(
            StringBuilder jpql,
            String fieldName,
            Long value
    ) {
        if (value == null) {
            jpql.append(" and property.")
                    .append(fieldName)
                    .append(" is null");
            return;
        }
        jpql.append(" and property.")
                .append(fieldName)
                .append(" = :")
                .append(fieldName);
    }

    private void setNullablePriceParameter(
            TypedQuery<Long> query,
            String parameterName,
            Long value
    ) {
        if (value != null) {
            query.setParameter(parameterName, value);
        }
    }

    private record AddressCondition(
            String expression,
            String value
    ) {
    }
}
