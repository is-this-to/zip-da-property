package com.zipdaproperty.domain.report.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAdminAssignmentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminAssignmentRequest(3001L, 0L)
        )).isEmpty();
    }

    @Test
    void missingFields_haveViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminAssignmentRequest(null, null)
        )).hasSize(2);
    }

    @Test
    void nonPositiveAdminIdOrNegativeVersion_haveViolations() {
        assertThat(validator.validate(
                new PropertyReportAdminAssignmentRequest(0L, -1L)
        )).hasSize(2);
    }
}
