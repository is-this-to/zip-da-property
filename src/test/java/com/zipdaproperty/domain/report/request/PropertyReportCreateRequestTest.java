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

import java.util.List;

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
                "가".repeat(length),
                null
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
                "가".repeat(1001),
                null
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
                "가".repeat(length),
                null
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void validate_nullReasonCode_rejectsRequest() {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                null,
                "신고 상세 내용이 열 자 이상입니다.",
                null
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("reasonCode"));
    }

    @Test
    void deserialize_missingNullOrEmptyEvidenceFileIds_acceptsRequest() {
        PropertyReportCreateRequest missing = objectMapper.readValue(
                requestJsonWithoutEvidence(),
                PropertyReportCreateRequest.class
        );
        PropertyReportCreateRequest nullEvidence = readRequestWithEvidence("null");
        PropertyReportCreateRequest emptyEvidence = readRequestWithEvidence("[]");

        assertThat(missing.evidenceFileIds()).isNull();
        assertThat(nullEvidence.evidenceFileIds()).isNull();
        assertThat(emptyEvidence.evidenceFileIds()).isEmpty();
        assertThat(VALIDATOR.validate(missing)).isEmpty();
        assertThat(VALIDATOR.validate(nullEvidence)).isEmpty();
        assertThat(VALIDATOR.validate(emptyEvidence)).isEmpty();
    }

    @Test
    void deserialize_oneToFiveTsidStrings_acceptsAndSerializesAsStrings() {
        PropertyReportCreateRequest one = readRequestWithEvidence(
                "[\"884700000000000001\"]"
        );
        PropertyReportCreateRequest five = readRequestWithEvidence(
                "[\"884700000000000001\",\"884700000000000002\","
                        + "\"884700000000000003\",\"884700000000000004\","
                        + "\"884700000000000005\"]"
        );

        assertThat(one.evidenceFileIds())
                .containsExactly(884700000000000001L);
        assertThat(five.evidenceFileIds()).hasSize(5);
        assertThat(VALIDATOR.validate(one)).isEmpty();
        assertThat(VALIDATOR.validate(five)).isEmpty();
        assertThat(objectMapper.writeValueAsString(one))
                .contains("\"evidenceFileIds\":[\"884700000000000001\"]");
    }

    @Test
    void validate_sixEvidenceFileIds_rejectsRequest() {
        PropertyReportCreateRequest request = new PropertyReportCreateRequest(
                ReportReasonCode.FALSE_INFO,
                "신고 상세 내용이 열 자 이상입니다.",
                List.of(1L, 2L, 3L, 4L, 5L, 6L)
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("evidenceFileIds"));
    }

    @Test
    void validate_nullInsideEvidenceFileIds_rejectsRequest() {
        PropertyReportCreateRequest request = readRequestWithEvidence(
                "[\"884700000000000001\",null]"
        );

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString()
                        .equals("evidenceFileIds[1].<list element>"));
    }

    @Test
    void deserialize_numericEvidenceFileId_rejectsRequest() {
        assertThatThrownBy(() -> readRequestWithEvidence(
                "[884700000000000001]"
        )).isInstanceOf(JacksonException.class);
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

    private PropertyReportCreateRequest readRequestWithEvidence(
            String evidenceFileIds
    ) {
        return objectMapper.readValue(
                """
                        {
                          "reasonCode": "FALSE_INFO",
                          "detail": "신고 상세 내용이 열 자 이상입니다.",
                          "evidenceFileIds": %s
                        }
                        """.formatted(evidenceFileIds),
                PropertyReportCreateRequest.class
        );
    }

    private String requestJsonWithoutEvidence() {
        return """
                {
                  "reasonCode": "FALSE_INFO",
                  "detail": "신고 상세 내용이 열 자 이상입니다."
                }
                """;
    }
}
