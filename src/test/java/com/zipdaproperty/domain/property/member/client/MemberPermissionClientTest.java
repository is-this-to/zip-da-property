package com.zipdaproperty.domain.property.member.client;

import com.zipdaproperty.domain.property.member.constant.MemberPermissionAction;
import com.zipdaproperty.global.context.TraceIdContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MemberPermissionClientTest {

    private MockRestServiceServer server;
    private MemberPermissionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://member.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MemberPermissionClient(builder.build());
        TraceIdContext.set("member-permission-test-trace");
    }

    @AfterEach
    void tearDown() {
        server.verify();
        TraceIdContext.clear();
    }

    @Test
    void getPermission_success_returnsAllowedAndReason() {
        server.expect(requestTo(
                        "http://member.test/internal/members/1001/permissions?action=PROPERTY_CREATE"
                ))
                .andExpect(header(
                        "X-Trace-Id",
                        "member-permission-test-trace"
                ))
                .andRespond(withSuccess(
                        """
                        {
                          "code": "00",
                          "message": "SUCCESS",
                          "data": {
                            "allowed": true,
                            "reason": null
                          },
                          "traceId": "member-trace"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        MemberPermissionResult result = client.getPermission(
                1001L,
                MemberPermissionAction.PROPERTY_CREATE
        );

        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    void getPermission_serverError_throwsUnavailable() {
        server.expect(requestTo(
                        "http://member.test/internal/members/1001/permissions?action=PROPERTY_UPDATE"
                ))
                .andRespond(withServerError());

        assertUnavailable(() -> client.getPermission(
                1001L,
                MemberPermissionAction.PROPERTY_UPDATE
        ));
    }

    @Test
    void getPermission_invalidBody_throwsUnavailable() {
        server.expect(requestTo(
                        "http://member.test/internal/members/1001/permissions?action=PROPERTY_CREATE"
                ))
                .andRespond(withSuccess(
                        "{\"code\":\"00\",\"data\":null}",
                        MediaType.APPLICATION_JSON
                ));

        assertUnavailable(() -> client.getPermission(
                1001L,
                MemberPermissionAction.PROPERTY_CREATE
        ));
    }

    private void assertUnavailable(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception)
                                .getCustomResponseCode()
                )
                .isEqualTo(
                        CustomResponseCode.MEMBER_API_UNAVAILABLE
                );
    }
}
