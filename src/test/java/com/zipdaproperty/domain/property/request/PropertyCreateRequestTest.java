package com.zipdaproperty.domain.property.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyCreateRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void toCommand_stringFileIds_preservesIdsAndOrder() {
        PropertyCreateRequest request = readRequest("[\"1003\",\"1001\",\"1002\"]");

        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.toCommand().fileIds()).containsExactly(1003L, 1001L, 1002L);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(request)).get("fileIds").get(0).isString())
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void validate_allowedFileCount_acceptsRequest(int count) {
        assertThat(VALIDATOR.validate(readRequest(fileIdsJson(count)))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[null]"})
    void validate_missingOrEmptyFileIds_rejectsRequest(String fileIds) {
        assertThat(VALIDATOR.validate(readRequest(fileIds))).isNotEmpty();
    }

    @Test
    void validate_absentFileIds_rejectsRequest() {
        PropertyCreateRequest request = objectMapper.readValue("{}", PropertyCreateRequest.class);

        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("fileIds"));
    }

    @Test
    void validate_tooManyFileIds_rejectsRequest() {
        assertThat(VALIDATOR.validate(readRequest(fileIdsJson(31)))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1001]", "[\"0\"]", "[\"-1\"]", "[\"\"]", "[\"abc\"]",
            "[\"9223372036854775808\"]", "[\"1.5\"]"})
    void deserialize_invalidTsid_rejectsRequest(String fileIds) {
        assertThatThrownBy(() -> readRequest(fileIds)).isInstanceOf(JacksonException.class);
    }

    private PropertyCreateRequest readRequest(String fileIds) {
        return objectMapper.readValue("""
                {
                  "regionId": "10",
                  "publisherType": "DIRECT_OWNER",
                  "propertyType": "APARTMENT",
                  "transactionType": "SALE",
                  "salePrice": 100000,
                  "exclusiveArea": 84.00,
                  "title": "등록 테스트",
                  "description": "매물 등록 테스트 설명",
                  "fileIds": %s
                }
                """.formatted(fileIds), PropertyCreateRequest.class);
    }

    private String fileIdsJson(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
