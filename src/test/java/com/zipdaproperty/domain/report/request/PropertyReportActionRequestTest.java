package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportActionCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyReportActionRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR =
            VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void request_containsOnlyActionCodeAndReason() {
        assertThat(Arrays.stream(
                PropertyReportActionRequest.class.getRecordComponents()
        ).map(RecordComponent::getName)).containsExactly(
                "actionCode",
                "reason"
        );
    }

    @ParameterizedTest
    @EnumSource(ReportActionCode.class)
    void validate_supportedActionCodeAndReason_acceptsRequest(
            ReportActionCode actionCode
    ) {
        PropertyReportActionRequest request =
                new PropertyReportActionRequest(
                        actionCode,
                        "운영조치 사유"
                );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void validate_missingActionCode_rejectsRequest() {
        PropertyReportActionRequest request =
                new PropertyReportActionRequest(
                        null,
                        "운영조치 사유"
                );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("actionCode"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validate_missingReason_rejectsRequest(String reason) {
        PropertyReportActionRequest request =
                new PropertyReportActionRequest(
                        ReportActionCode.HIDE_PROPERTY,
                        reason
                );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("reason"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    void validate_reasonAtLengthBoundaries_acceptsRequest(int length) {
        PropertyReportActionRequest request =
                new PropertyReportActionRequest(
                        ReportActionCode.HIDE_PROPERTY,
                        "가".repeat(length)
                );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void validate_reasonLongerThanOneThousandCharacters_rejectsRequest() {
        PropertyReportActionRequest request =
                new PropertyReportActionRequest(
                        ReportActionCode.HIDE_PROPERTY,
                        "가".repeat(1001)
                );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("reason"));
    }
}
