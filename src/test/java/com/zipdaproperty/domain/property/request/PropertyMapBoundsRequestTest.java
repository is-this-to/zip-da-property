package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapBoundsRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    @DisplayName("복수 매물·거래 유형 요청을 검색 조건으로 변환한다")
    void convertRequestToSearchCondition() {
        PropertyMapBoundsRequest request = request(
                Set.of(
                        PropertyType.APARTMENT,
                        PropertyType.ROOM
                ),
                Set.of(
                        TransactionType.JEONSE,
                        TransactionType.MONTHLY_RENT
                ),
                100_000_000L,
                PropertyMapSort.PRICE_ASC
        );

        assertThat(request.toSearchCondition().propertyTypes())
                .containsExactlyInAnyOrder(
                        PropertyType.APARTMENT,
                        PropertyType.ROOM
                );
        assertThat(request.toSearchCondition().transactionTypes())
                .containsExactlyInAnyOrder(
                        TransactionType.JEONSE,
                        TransactionType.MONTHLY_RENT
                );
        assertThat(request.toSearchCondition().minDeposit())
                .isEqualTo(100_000_000L);
        assertThat(request.toSearchCondition().sort())
                .isEqualTo(PropertyMapSort.PRICE_ASC);
    }

    @Test
    @DisplayName("음수 가격 요청은 Bean Validation에서 거부한다")
    void rejectNegativePrice() {
        PropertyMapBoundsRequest request = request(
                null,
                null,
                -1L,
                null
        );

        Set<ConstraintViolation<PropertyMapBoundsRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getPropertyPath()
                                .toString()
                                .equals("minDeposit")
                );
    }

    private PropertyMapBoundsRequest request(
            Set<PropertyType> propertyTypes,
            Set<TransactionType> transactionTypes,
            Long minDeposit,
            PropertyMapSort sort
    ) {
        return new PropertyMapBoundsRequest(
                37.45,
                37.55,
                126.95,
                127.10,
                6,
                propertyTypes,
                transactionTypes,
                null,
                null,
                minDeposit,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sort
        );
    }
}
