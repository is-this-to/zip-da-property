package com.zipdaproperty.global.config.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PropertyOpenApiContractTest {

    private static final String PROPERTY_PATH =
            "/api/property/properties";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private JsonNode paths;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        String openApiJson = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        paths = objectMapper.readTree(openApiJson).path("paths");
    }

    @Test
    void propertyWriteOperations_publishActualSuccessStatusCodes() {
        assertSuccessResponse(PROPERTY_PATH, "post", "201");
        assertSuccessResponse(
                PROPERTY_PATH + "/{propertyId}",
                "patch",
                "200"
        );
        assertSuccessResponse(
                PROPERTY_PATH + "/{propertyId}",
                "delete",
                "204"
        );
        assertSuccessResponse(
                PROPERTY_PATH + "/{propertyId}/transaction-status",
                "patch",
                "200"
        );
        assertSuccessResponse(
                PROPERTY_PATH + "/{propertyId}/publication-status",
                "patch",
                "200"
        );
        assertSuccessResponse(
                PROPERTY_PATH + "/{propertyId}/restore",
                "post",
                "200"
        );
    }

    @Test
    void propertyVerificationOperations_publishActualSuccessStatusCodes() {
        String propertyPath = PROPERTY_PATH + "/{propertyId}";

        assertSuccessResponse(
                propertyPath + "/verifications",
                "post",
                "201"
        );
        assertSuccessResponse(
                propertyPath + "/verifications/owner",
                "post",
                "202"
        );
        assertSuccessResponse(
                propertyPath + "/verifications/tenant",
                "post",
                "202"
        );
        assertSuccessResponse(
                propertyPath + "/reverification",
                "post",
                "202"
        );
        assertSuccessResponse(
                propertyPath + "/verifications/{verificationId}",
                "patch",
                "200"
        );
    }

    @Test
    void propertyWriteOperations_publishRequiredConcurrencyHeadersAndErrors() {
        JsonNode create = operation(PROPERTY_PATH, "post");
        JsonNode update = operation(
                PROPERTY_PATH + "/{propertyId}",
                "patch"
        );

        assertRequiredHeader(create, "Idempotency-Key");
        assertRequiredHeader(update, "If-Match");

        assertThat(create.path("responses").has("400")).isTrue();
        assertThat(create.path("responses").has("409")).isTrue();
        assertThat(update.path("responses").has("400")).isTrue();
        assertThat(update.path("responses").has("409")).isTrue();
    }

    private void assertSuccessResponse(
            String path,
            String method,
            String expectedStatus
    ) {
        JsonNode responses = operation(path, method).path("responses");

        assertThat(responses.has(expectedStatus))
                .as("%s %s response %s", method, path, expectedStatus)
                .isTrue();

        if (!"200".equals(expectedStatus)) {
            assertThat(responses.has("200"))
                    .as("%s %s must not expose a false 200 response", method, path)
                    .isFalse();
        }
    }

    private JsonNode operation(String path, String method) {
        JsonNode operation = paths.path(path).path(method);

        assertThat(operation.isMissingNode())
                .as("OpenAPI operation %s %s", method, path)
                .isFalse();

        return operation;
    }

    private void assertRequiredHeader(
            JsonNode operation,
            String headerName
    ) {
        boolean requiredHeaderExists = StreamSupport.stream(
                        operation.path("parameters").spliterator(),
                        false
                )
                .anyMatch(parameter ->
                        headerName.equals(parameter.path("name").asText())
                                && "header".equals(
                                parameter.path("in").asText()
                        )
                                && parameter.path("required").asBoolean()
                );

        assertThat(requiredHeaderExists)
                .as("required OpenAPI header %s", headerName)
                .isTrue();
    }
}
