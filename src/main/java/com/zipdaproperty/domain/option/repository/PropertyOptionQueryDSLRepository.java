package com.zipdaproperty.domain.option.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.property.constant.PropertyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.zipdaproperty.domain.option.entity.QPropertyOption.propertyOption;
import static com.zipdaproperty.domain.option.entity.QPropertyOptionCode.propertyOptionCode;
import static com.zipdaproperty.domain.option.entity.QPropertyTypeOption.propertyTypeOption;

@Repository
@RequiredArgsConstructor
public class PropertyOptionQueryDSLRepository {

    private final JPAQueryFactory queryFactory;

    public List<PropertyOption> findActiveOptionsByPropertyId(Long propertyId) {
        return queryFactory.selectFrom(propertyOption)
                .where(
                        propertyOption.propertyId.eq(propertyId),
                        propertyOption.deletedAt.isNull()
                )
                .orderBy(propertyOption.displayOrder.asc(), propertyOption.propertyOptionId.asc())
                .fetch();
    }

    public List<PropertyOption> findActiveOptionsByPropertyIdAndOptionCodeId(
            Long propertyId,
            Long optionCodeId
    ) {
        return queryFactory.selectFrom(propertyOption)
                .where(
                        propertyOption.propertyId.eq(propertyId),
                        propertyOption.optionCodeId.eq(optionCodeId),
                        propertyOption.deletedAt.isNull()
                )
                .orderBy(propertyOption.displayOrder.asc(), propertyOption.propertyOptionId.asc())
                .fetch();
    }

    public List<PropertyTypeOption> findActiveTypeOptions(PropertyType propertyType) {
        return queryFactory.selectFrom(propertyTypeOption)
                .where(
                        propertyTypeOption.propertyType.eq(propertyType),
                        propertyTypeOption.deletedAt.isNull()
                )
                .orderBy(propertyTypeOption.displayOrder.asc(), propertyTypeOption.propertyTypeOptionId.asc())
                .fetch();
    }

    public List<PropertyOptionCode> findActiveOptionCodesByIds(Collection<Long> optionCodeIds) {
        if (optionCodeIds.isEmpty()) {
            return List.of();
        }

        return queryFactory.selectFrom(propertyOptionCode)
                .where(
                        propertyOptionCode.optionCodeId.in(optionCodeIds),
                        propertyOptionCode.deletedAt.isNull(),
                        propertyOptionCode.active.isTrue()
                )
                .fetch();
    }

    public Optional<PropertyOptionCode> findActiveOptionCode(String optionCode) {
        return Optional.ofNullable(
                queryFactory.selectFrom(propertyOptionCode)
                        .where(
                                propertyOptionCode.optionCode.eq(optionCode),
                                propertyOptionCode.deletedAt.isNull(),
                                propertyOptionCode.active.isTrue()
                        )
                        .fetchOne()
        );
    }
    public boolean existsActiveOption(
            Long propertyId,
            Long optionCodeId
    ) {
        return queryFactory
                .selectOne()
                .from(propertyOption)
                .where(
                        propertyOption.propertyId.eq(propertyId),
                        propertyOption.optionCodeId.eq(optionCodeId),
                        propertyOption.deletedAt.isNull()
                )
                .fetchFirst() != null;
    }

    public boolean existsActiveTypeOption(
            PropertyType propertyType,
            Long optionCodeId
    ) {
        return queryFactory
                .selectOne()
                .from(propertyTypeOption)
                .where(
                        propertyTypeOption.propertyType.eq(propertyType),
                        propertyTypeOption.optionCodeId.eq(optionCodeId),
                        propertyTypeOption.deletedAt.isNull()
                )
                .fetchFirst() != null;
    }
}
