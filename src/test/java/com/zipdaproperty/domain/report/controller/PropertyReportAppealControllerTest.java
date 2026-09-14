package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAppealCreateRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAppealCreateResponse;
import com.zipdaproperty.domain.report.service.PropertyReportAppealService;
import com.zipdaproperty.domain.report.type.ReportStatus;
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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyReportAppealControllerTest {

    private static final Long REPORT_ID = 884685586571263701L;
    private static final String DETAIL = "운영조치에 이의를 신청하는 상세 사유입니다.";

    private final PropertyReportAppealService appealService =
            mock(PropertyReportAppealService.class);
    private final ActorContext actorContext = ActorContext.member(
            2001L,
            ActorRole.USER,
            "appeal-controller-test"
    );
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PropertyReportAppealController(appealService)
                )
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void createAppeal_validRequest_returnsCreatedWithoutAppealId()
            throws Exception {
        when(appealService.createAppeal(REPORT_ID, DETAIL, actorContext))
                .thenReturn(new PropertyReportAppealCreateResponse(
                        REPORT_ID,
                        ReportStatus.ACTIONED,
                        3L
                ));

        mockMvc.perform(post(
                        "/api/property-reports/{reportId}/appeals",
                        REPORT_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detail\":\"" + DETAIL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(REPORT_ID))
                .andExpect(jsonPath("$.data.status").value("ACTIONED"))
                .andExpect(jsonPath("$.data.version").value(3L))
                .andExpect(jsonPath("$.data.appealId").doesNotExist());

        verify(appealService).createAppeal(REPORT_ID, DETAIL, actorContext);
    }

    @Test
    void createAppeal_endpointRequiresUserOrAgent() throws Exception {
        Method method = PropertyReportAppealController.class.getDeclaredMethod(
                "createAppeal",
                Long.class,
                PropertyReportAppealCreateRequest.class,
                ActorContext.class
        );

        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/{reportId}/appeals");
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('USER', 'AGENT')");
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
                return actorContext;
            }
        };
    }
}
