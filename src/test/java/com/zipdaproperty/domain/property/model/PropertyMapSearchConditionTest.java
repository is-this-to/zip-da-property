package com.zipdaproperty.domain.property.model;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyMapSearchConditionTest {

    @Test
    @DisplayName("선택하지 않은 필터는 빈 조건과 최신순으로 정규화한다")
    void normalizeEmptyFiltersAndDefaultSort() {
        PropertyMapSearchCondition condition =
                conditionBuilder().build();

        assertThat(condition.propertyTypes()).isEmpty();
        assertThat(condition.transactionTypes()).isEmpty();
        assertThat(condition.publisherTypes()).isEmpty();
        assertThat(condition.sort())
                .isEqualTo(PropertyMapSort.LATEST);
    }

    @Test
    @DisplayName("선택한 매물·거래 유형과 정렬을 보존한다")
    void preserveSelectedFilters() {
        PropertyMapSearchCondition condition =
                conditionBuilder()
                        .propertyTypes(
                                Set.of(
                                        PropertyType.APARTMENT,
                                        PropertyType.ROOM
                                )
                        )
                        .transactionTypes(
                                Set.of(
                                        TransactionType.JEONSE,
                                        TransactionType.MONTHLY_RENT
                                )
                        )
                        .sort(PropertyMapSort.PRICE_ASC)
                        .build();

        assertThat(condition.propertyTypes())
                .containsExactlyInAnyOrder(
                        PropertyType.APARTMENT,
                        PropertyType.ROOM
                );
        assertThat(condition.transactionTypes())
                .containsExactlyInAnyOrder(
                        TransactionType.JEONSE,
                        TransactionType.MONTHLY_RENT
                );
        assertThat(condition.sort())
                .isEqualTo(PropertyMapSort.PRICE_ASC);
    }

    @Test
    @DisplayName("가격 최솟값이 최댓값보다 크면 거부한다")
    void rejectInvalidPriceRange() {
        assertThatThrownBy(() ->
                conditionBuilder()
                        .minSalePrice(500_000_000L)
                        .maxSalePrice(300_000_000L)
                        .build()
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("매매가");
    }

    @Test
    @DisplayName("0 이하 전용면적은 거부한다")
    void rejectNonPositiveExclusiveArea() {
        assertThatThrownBy(() ->
                conditionBuilder()
                        .minExclusiveArea(BigDecimal.ZERO)
                        .build()
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("전용면적");
    }

    @Test
    @DisplayName("사용승인일 시작일이 종료일보다 늦으면 거부한다")
    void rejectInvalidApprovalDateRange() {
        assertThatThrownBy(() ->
                conditionBuilder()
                        .approvalDateFrom(
                                LocalDate.of(2020, 1, 1)
                        )
                        .approvalDateTo(
                                LocalDate.of(2010, 1, 1)
                        )
                        .build()
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용승인일");
    }

    private ConditionBuilder conditionBuilder() {
        return new ConditionBuilder();
    }

    private static class ConditionBuilder {

        private Set<PropertyType> propertyTypes;
        private Set<TransactionType> transactionTypes;
        private Long minSalePrice;
        private Long maxSalePrice;
        private BigDecimal minExclusiveArea;
        private LocalDate approvalDateFrom;
        private LocalDate approvalDateTo;
        private PropertyMapSort sort;

        private ConditionBuilder propertyTypes(
                Set<PropertyType> propertyTypes
        ) {
            this.propertyTypes = propertyTypes;
            return this;
        }

        private ConditionBuilder transactionTypes(
                Set<TransactionType> transactionTypes
        ) {
            this.transactionTypes = transactionTypes;
            return this;
        }

        private ConditionBuilder minSalePrice(Long minSalePrice) {
            this.minSalePrice = minSalePrice;
            return this;
        }

        private ConditionBuilder maxSalePrice(Long maxSalePrice) {
            this.maxSalePrice = maxSalePrice;
            return this;
        }

        private ConditionBuilder minExclusiveArea(
                BigDecimal minExclusiveArea
        ) {
            this.minExclusiveArea = minExclusiveArea;
            return this;
        }

        private ConditionBuilder approvalDateFrom(
                LocalDate approvalDateFrom
        ) {
            this.approvalDateFrom = approvalDateFrom;
            return this;
        }

        private ConditionBuilder approvalDateTo(
                LocalDate approvalDateTo
        ) {
            this.approvalDateTo = approvalDateTo;
            return this;
        }

        private ConditionBuilder sort(PropertyMapSort sort) {
            this.sort = sort;
            return this;
        }

        private PropertyMapSearchCondition build() {
            return new PropertyMapSearchCondition(
                    propertyTypes,
                    transactionTypes,
                    minSalePrice,
                    maxSalePrice,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    minExclusiveArea,
                    null,
                    null,
                    null,
                    null,
                    approvalDateFrom,
                    approvalDateTo,
                    null,
                    null,
                    null,
                    sort
            );
        }
    }
}
