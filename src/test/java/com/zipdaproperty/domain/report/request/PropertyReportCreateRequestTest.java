package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyReportCreateRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @ParameterizedTest
    @EnumSource(ReportReasonCode.class)
    void deserialize_supportedReasonCode_acceptsRequest(
            ReportReasonCode reasonCode
    ) {
        PropertyReportCreateRequest request = readRequest(
                reasonCode.name(),
                "신고 상세 내용이 열 자 이상입니다."
        );

        assertThat(request.reasonCode()).isEqualTo(reasonCode);
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void deserialize_unknownReasonCode_rejectsRequest() {
        assertThatThrownBy(() -> readRequest(
                "NOT_SUPPORTED_REASON",
                "신고 상세 내용이 열 자 이상입니다."
        )).isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9})
    void validate_detailShorterThanTenCharacters_rejectsRequest(int length) {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                ReportReasonCode.FALSE_INFO,
                "가".repeat(length)
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("detail"));
    }

    @Test
    void validate_detailLongerThanOneThousandCharacters_rejectsRequest() {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                ReportReasonCode.FALSE_INFO,
                "가".repeat(1001)
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("detail"));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 1000})
    void validate_detailAtLengthBoundaries_acceptsRequest(int length) {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                ReportReasonCode.FALSE_INFO,
                "가".repeat(length)
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void validate_nullReasonCode_rejectsRequest() {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                null,
                "신고 상세 내용이 열 자 이상입니다."
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("reasonCode"));
    }

    private PropertyReportCreateRequest readRequest(
            String reasonCode,
            String detail
    ) {
        return objectMapper.readValue(
                """
                        {
                          "reasonCode": "%s",
                          "detail": "%s"
                        }
                        """.formatted(reasonCode, detail),
                PropertyReportCreateRequest.class
        );
    }
}
