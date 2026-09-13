package com.zipdaproperty.domain.property.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyUpdateRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR =
            VALIDATOR_FACTORY.getValidator();

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void deserialize_topLevelStringFileIds_preservesIdsAndOrder() {
        PropertyUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "version": 3,
                          "changes": {},
                          "fileIds": ["103", "101", "104"]
                        }
                        """,
                PropertyUpdateRequest.class
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.fileIds()).containsExactly(103L, 101L, 104L);
        assertThat(objectMapper.readTree(
                objectMapper.writeValueAsString(request)
        ).get("fileIds").get(0).isString()).isTrue();
    }

    @Test
    void validate_emptyChangesWithoutFileIds_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(),
                null
        );

        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void validate_optionsOnly_acceptsNonEmptyAndEmptyLists() {
        for (String options : List.of("[]", "[{\"optionCode\":\"PARKING\",\"optionValue\":\"2\"}]")) {
            PropertyUpdateRequest request = objectMapper.readValue(
                    "{\"version\":3,\"options\":" + options + "}", PropertyUpdateRequest.class);
            assertThat(request.isUpdateTargetProvided()).isTrue();
            assertThat(VALIDATOR.validate(request)).isEmpty();
            assertThat(request.fileIds()).isNull();
        }
    }

    @Test
    void validate_nullOptionsWithoutOtherTargets_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(3L, null, null, null, null);
        assertThat(request.isUpdateTargetProvided()).isFalse();
        assertThat(VALIDATOR.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("updateTargetProvided"));
    }

    @Test
    void validate_emptyFileIds_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(),
                List.of()
        );

        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void validate_propertyChangesWithoutFileIds_accepts() {
        PropertyUpdateRequest request = objectMapper.readValue(
                """
                        {
                          "version": 3,
                          "changes": {"title": "수정 제목"}
                        }
                        """,
                PropertyUpdateRequest.class
        );

        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.fileIds()).isNull();
    }

    @Test
    void validate_thirtyOneFileIds_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(),
                java.util.stream.LongStream.rangeClosed(1, 31)
                        .boxed()
                        .toList()
        );

        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void validate_nullFileIdElement_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(),
                java.util.Arrays.asList(101L, null)
        );

        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void deserialize_nonStringOrInvalidTsid_rejects() {
        assertThatThrownBy(() -> objectMapper.readValue(
                """
                        {
                          "version": 3,
                          "changes": {},
                          "fileIds": ["not-a-tsid"]
                        }
                        """,
                PropertyUpdateRequest.class
        )).isInstanceOf(RuntimeException.class);
    }
}
