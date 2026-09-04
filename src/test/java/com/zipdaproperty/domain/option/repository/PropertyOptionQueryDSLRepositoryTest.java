package com.zipdaproperty.domain.option.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.property.constant.PropertyType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.zipdaproperty.domain.option.entity.QPropertyOption.propertyOption;
import static com.zipdaproperty.domain.option.entity.QPropertyOptionCode.propertyOptionCode;
import static com.zipdaproperty.domain.option.entity.QPropertyTypeOption.propertyTypeOption;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyOptionQueryDSLRepositoryTest {

    private final JPAQueryFactory factory = mock(JPAQueryFactory.class);
    private final PropertyOptionQueryDSLRepository repository = new PropertyOptionQueryDSLRepository(factory);

    @Test
    void findActiveOptionsByPropertyId_buildsIdAndSoftDeleteQuery() {
        JPAQuery<PropertyOption> query = new CapturingQuery<>();
        when(factory.selectFrom(propertyOption)).thenReturn(query);

        repository.findActiveOptionsByPropertyId(10L);

        assertThat(query.getMetadata().getWhere().toString())
                .contains("propertyOption.propertyId = 10", "propertyOption.deletedAt is null");
        assertThat(query.getMetadata().getOrderBy())
                .containsExactly(propertyOption.displayOrder.asc(), propertyOption.propertyOptionId.asc());
    }

    @Test
    void findActiveOptionsByPropertyIdAndOptionCodeId_keepsBothIdPredicates() {
        JPAQuery<PropertyOption> query = new CapturingQuery<>();
        when(factory.selectFrom(propertyOption)).thenReturn(query);

        assertThat(repository.findActiveOptionsByPropertyIdAndOptionCodeId(10L, 20L)).isEmpty();

        assertThat(query.getMetadata().getWhere().toString()).contains(
                "propertyOption.propertyId = 10",
                "propertyOption.optionCodeId = 20",
                "propertyOption.deletedAt is null"
        );
    }

    @Test
    void findActiveTypeOptions_filtersTypeAndSoftDelete() {
        JPAQuery<PropertyTypeOption> query = new CapturingQuery<>();
        when(factory.selectFrom(propertyTypeOption)).thenReturn(query);

        repository.findActiveTypeOptions(PropertyType.ROOM);

        assertThat(query.getMetadata().getWhere().toString())
                .contains("propertyTypeOption.propertyType = ROOM", "propertyTypeOption.deletedAt is null");
        assertThat(query.getMetadata().getOrderBy())
                .containsExactly(propertyTypeOption.displayOrder.asc(), propertyTypeOption.propertyTypeOptionId.asc());
    }

    @Test
    void findActiveOptionCodesByIds_emptyIds_skipsQuery() {
        assertThat(repository.findActiveOptionCodesByIds(List.of())).isEmpty();
        verifyNoInteractions(factory);
    }

    @Test
    void findActiveOptionCodesByIds_filtersActiveWithoutCapabilityRestrictions() {
        JPAQuery<PropertyOptionCode> query = new CapturingQuery<>();
        when(factory.selectFrom(propertyOptionCode)).thenReturn(query);

        repository.findActiveOptionCodesByIds(List.of(10L, 20L));

        assertThat(query.getMetadata().getWhere().toString())
                .contains("propertyOptionCode.optionCodeId in [10, 20]",
                        "propertyOptionCode.deletedAt is null", "propertyOptionCode.active = true")
                .doesNotContain("filterable", "registrationEnabled");
    }

    @Test
    void findActiveOptionCode_noResult_returnsEmptyOptional() {
        JPAQuery<PropertyOptionCode> query = new CapturingQuery<>();
        when(factory.selectFrom(propertyOptionCode)).thenReturn(query);

        assertThat(repository.findActiveOptionCode("AIR_CONDITIONER")).isEmpty();
        assertThat(query.getMetadata().getWhere().toString()).contains(
                "propertyOptionCode.optionCode = AIR_CONDITIONER",
                "propertyOptionCode.deletedAt is null", "propertyOptionCode.active = true"
        );
    }
    // DB는 호출하지 않고 실제 QueryDSL 조건과 정렬 메타데이터만 검사한다.
    private static class CapturingQuery<T> extends JPAQuery<T> {
        @Override
        public List<T> fetch() {
            return List.of();
        }

        @Override
        public T fetchOne() {
            return null;
        }
    }
}
