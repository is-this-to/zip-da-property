package com.zipdaproperty.domain.report.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAdminRiskScoreRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100", "42.50"})
    void scoreWithinRange_hasNoViolations(String value) {
        assertThat(validator.validate(
                new PropertyReportAdminRiskScoreRequest(new BigDecimal(value), 0L)
        )).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01"})
    void scoreOutsideRange_hasViolation(String value) {
        assertThat(validator.validate(
                new PropertyReportAdminRiskScoreRequest(new BigDecimal(value), 0L)
        )).isNotEmpty();
    }

    @Test
    void missingFields_haveViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminRiskScoreRequest(null, null)
        )).hasSize(2);
    }
}
