package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "PROPERTY_MAP_QUERY_TEST_ENABLED",
        matches = "true"
)
@SpringBootTest
@Transactional(readOnly = true)
class PropertyMapQueryRepositoryIntegrationTest {

    @Autowired
    private PropertyMapBoundsQueryRepository boundsQueryRepository;

    @Autowired
    private PropertyMapRegionAggregateQueryRepository
            regionAggregateQueryRepository;

    @Test
    @DisplayName("지도 필터·정렬 쿼리가 MySQL 공간 스키마에서 실행된다")
    void mapFilterQueriesExecuteAgainstMySqlSchema() {
        PropertyMapBounds bounds = new PropertyMapBounds(
                37.45,
                37.55,
                126.95,
                127.10,
                6
        );

        for (PropertyMapSort sort : PropertyMapSort.values()) {
            PropertyMapSearchCondition condition =
                    condition(sort);

            assertThat(
                    boundsQueryRepository
                            .countPublicPropertiesInBounds(
                                    bounds,
                                    condition
                            )
            ).isNotNegative();

            assertThat(
                    boundsQueryRepository
                            .findPublicPropertiesInBounds(
                                    bounds,
                                    condition,
                                    1
                            )
            ).isNotNull();
        }

        assertThat(
                regionAggregateQueryRepository.findRegionAggregates(
                        bounds,
                        condition(PropertyMapSort.LATEST),
                        1
                )
        ).isNotNull();
    }

    private PropertyMapSearchCondition condition(
            PropertyMapSort sort
    ) {
        return new PropertyMapSearchCondition(
                Set.of(),
                Set.of(),
                0L,
                2_000_000_000L,
                0L,
                2_000_000_000L,
                0L,
                10_000_000L,
                0L,
                10_000_000L,
                new BigDecimal("1.0"),
                new BigDecimal("1000.0"),
                1,
                20,
                Set.of(),
                LocalDate.of(1900, 1, 1),
                LocalDate.of(2100, 12, 31),
                null,
                null,
                null,
                sort
        );
    }
}
