package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportActionRequest;
import com.zipdaproperty.domain.report.response.PropertyReportActionResponse;
import com.zipdaproperty.domain.report.service.PropertyReportActionService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminDetailService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminListService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminStatusChangeService;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyReportAdminControllerTest {

    private static final Long ACTION_ID = 501L;
    private static final Long REPORT_ID = 884685586571263701L;
    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final String REASON = "신고 검토에 따른 운영조치";
    private static final Instant EXECUTED_AT =
            Instant.parse("2026-09-13T12:00:00Z");

    private final PropertyReportActionService actionService =
            mock(PropertyReportActionService.class);
    private final ActorContext adminContext = ActorContext.member(
            3001L,
            ActorRole.CS_ADMIN,
            "property-report-action-controller-test"
    );

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PropertyReportAdminController controller =
                new PropertyReportAdminController(
                        mock(PropertyReportAdminListService.class),
                        mock(PropertyReportAdminDetailService.class),
                        mock(PropertyReportAdminStatusChangeService.class),
                        actionService
                );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void createAction_validRequest_returnsCreatedAndDelegates() throws Exception {
        PropertyReportActionResponse response =
                new PropertyReportActionResponse(
                        ACTION_ID,
                        REPORT_ID,
                        PROPERTY_ID,
                        ReportActionCode.HIDE_PROPERTY,
                        EXECUTED_AT
                );
        when(actionService.executeAction(
                REPORT_ID,
                new PropertyReportActionRequest(
                        ReportActionCode.HIDE_PROPERTY,
                        REASON
                ),
                adminContext
        )).thenReturn(response);

        mockMvc.perform(post(
                        "/api/admin/property-reports/{reportId}/actions",
                        REPORT_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionCode": "HIDE_PROPERTY",
                                  "reason": "신고 검토에 따른 운영조치"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.actionId").value(ACTION_ID))
                .andExpect(jsonPath("$.data.actionCode")
                        .value("HIDE_PROPERTY"));

        verify(actionService).executeAction(
                REPORT_ID,
                new PropertyReportActionRequest(
                        ReportActionCode.HIDE_PROPERTY,
                        REASON
                ),
                adminContext
        );
    }

    @Test
    void createAction_endpointRequiresCsOrSuperAdmin() throws Exception {
        Method method = PropertyReportAdminController.class.getDeclaredMethod(
                "createAction",
                Long.class,
                PropertyReportActionRequest.class,
                ActorContext.class
        );

        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/{reportId}/actions");
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')");
    }

    private HandlerMethodArgumentResolver actorContextResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType() == ActorContext.class;
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return adminContext;
            }
        };
    }
}
