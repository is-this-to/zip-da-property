package com.zipdaproperty.domain.report.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAdminListRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void nullSize_defaultsToTwenty() {
        PropertyReportAdminListRequest request = new PropertyReportAdminListRequest(null, null);

        assertThat(request.size()).isEqualTo(20);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void boundarySizes_areValid() {
        assertThat(validator.validate(new PropertyReportAdminListRequest(null, 1))).isEmpty();
        assertThat(validator.validate(new PropertyReportAdminListRequest(null, 50))).isEmpty();
    }

    @Test
    void outOfRangeSizes_areInvalid() {
        assertThat(validator.validate(new PropertyReportAdminListRequest(null, 0))).isNotEmpty();
        assertThat(validator.validate(new PropertyReportAdminListRequest(null, 51))).isNotEmpty();
    }
}
