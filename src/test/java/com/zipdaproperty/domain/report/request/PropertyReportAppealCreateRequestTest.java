package com.zipdaproperty.domain.report.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportAppealCreateRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR =
            VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 2000})
    void validate_detailAtBoundaries_acceptsRequest(int length) {
        PropertyReportAppealCreateRequest request =
                new PropertyReportAppealCreateRequest("가".repeat(length));

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {19, 2001})
    void validate_detailOutsideBoundaries_rejectsRequest(int length) {
        PropertyReportAppealCreateRequest request =
                new PropertyReportAppealCreateRequest("가".repeat(length));

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("detail"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validate_blankDetail_rejectsRequest(String detail) {
        PropertyReportAppealCreateRequest request =
                new PropertyReportAppealCreateRequest(detail);

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("detail"));
    }
}
