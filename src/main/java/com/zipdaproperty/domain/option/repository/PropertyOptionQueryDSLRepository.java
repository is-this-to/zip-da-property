package com.zipdaproperty.domain.option.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.response.PropertyDetailOptionResponse;
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

    public List<PropertyOptionCode> findActiveOptionCodesByCodes(Collection<String> optionCodes) {
        if (optionCodes.isEmpty()) {
            return List.of();
        }

        return queryFactory.selectFrom(propertyOptionCode)
                .where(
                        propertyOptionCode.optionCode.in(optionCodes),
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

    public List<PropertyEditOptionQueryRow> findEditOptions(
            Long propertyId,
            PropertyType propertyType
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PropertyEditOptionQueryRow.class,
                        propertyOptionCode.optionCode,
                        propertyOptionCode.optionName,
                        propertyOptionCode.optionCategory,
                        propertyOption.optionValue,
                        propertyTypeOption.required,
                        propertyOption.displayOrder
                ))
                .from(propertyOption)
                .join(propertyOptionCode)
                .on(propertyOptionCode.optionCodeId.eq(propertyOption.optionCodeId))
                .leftJoin(propertyTypeOption)
                .on(
                        propertyTypeOption.optionCodeId.eq(propertyOption.optionCodeId),
                        propertyTypeOption.propertyType.eq(propertyType),
                        propertyTypeOption.deletedAt.isNull()
                )
                .where(
                        propertyOption.propertyId.eq(propertyId),
                        propertyOption.deletedAt.isNull(),
                        propertyOptionCode.deletedAt.isNull(),
                        propertyOptionCode.active.isTrue()
                )
                .orderBy(
                        propertyOption.displayOrder.asc(),
                        propertyOption.propertyOptionId.asc()
                )
                .fetch();
    }

    public List<PropertyDetailOptionResponse> findDetailVisibleOptions(
            Long propertyId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PropertyDetailOptionResponse.class,
                        propertyOptionCode.optionCode,
                        propertyOptionCode.optionName,
                        propertyOptionCode.optionCategory,
                        propertyOption.optionValue,
                        propertyOption.displayOrder
                ))
                .from(propertyOption)
                .join(propertyOptionCode)
                .on(propertyOptionCode.optionCodeId.eq(propertyOption.optionCodeId))
                .where(
                        propertyOption.propertyId.eq(propertyId),
                        propertyOption.deletedAt.isNull(),
                        propertyOptionCode.deletedAt.isNull(),
                        propertyOptionCode.active.isTrue(),
                        propertyOptionCode.detailVisible.isTrue()
                )
                .orderBy(
                        propertyOption.displayOrder.asc(),
                        propertyOption.propertyOptionId.asc()
                )
                .fetch();
    }

    public boolean existsByPropertyIdAndOptionCodeIdAndDeletedAtIsNull(
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

    public boolean existsByPropertyTypeAndOptionCodeIdAndDeletedAtIsNull(
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
