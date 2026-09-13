package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAppealAdminReviewRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAppealAdminReviewResponse;
import com.zipdaproperty.domain.report.service.PropertyReportAppealAdminReviewService;
import com.zipdaproperty.domain.report.type.AppealStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyReportAppealAdminControllerTest {

    private static final Long APPEAL_ID = 41L;
    private final PropertyReportAppealAdminReviewService reviewService =
            mock(PropertyReportAppealAdminReviewService.class);
    private final ActorContext adminContext = ActorContext.member(
            3002L,
            ActorRole.CS_ADMIN,
            "appeal-admin-controller-test"
    );
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PropertyReportAppealAdminController(reviewService)
                )
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void review_validRequest_returnsOk() throws Exception {
        PropertyReportAppealAdminReviewRequest request =
                new PropertyReportAppealAdminReviewRequest(
                        AppealStatus.IN_REVIEW,
                        null,
                        0L
                );
        when(reviewService.review(APPEAL_ID, request, adminContext))
                .thenReturn(new PropertyReportAppealAdminReviewResponse(
                        APPEAL_ID,
                        AppealStatus.IN_REVIEW,
                        1L
                ));

        mockMvc.perform(patch(
                        "/api/admin/property-report-appeals/{appealId}",
                        APPEAL_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "IN_REVIEW",
                                  "reviewReason": null,
                                  "version": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appealId").value(APPEAL_ID))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.version").value(1L));

        verify(reviewService).review(APPEAL_ID, request, adminContext);
    }

    @Test
    void review_endpointRequiresCsOrSuperAdmin() throws Exception {
        Method method = PropertyReportAppealAdminController.class
                .getDeclaredMethod(
                        "review",
                        Long.class,
                        PropertyReportAppealAdminReviewRequest.class,
                        ActorContext.class
                );

        assertThat(method.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{appealId}");
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
