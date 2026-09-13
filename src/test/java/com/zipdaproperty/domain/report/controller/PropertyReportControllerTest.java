package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportCreateRequest;
import com.zipdaproperty.domain.report.response.PropertyReportCreateResponse;
import com.zipdaproperty.domain.report.service.PropertyReportService;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyReportControllerTest {

    private static final Long PROPERTY_ID = 884700000000000001L;
    private static final Long REPORT_ID = 884700000000000002L;
    private static final Long FILE_ID = 884700000000000003L;
    private static final String DETAIL = "허위 매물 정보가 포함되어 있어 신고합니다.";

    private final PropertyReportService service = mock(PropertyReportService.class);
    private final ActorContext actorContext = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-report-controller-test"
    );

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PropertyReportController(service))
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void createReport_tsidEvidenceStrings_returnsCreatedAndDelegates() throws Exception {
        when(service.createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                List.of(FILE_ID),
                actorContext
        )).thenReturn(new PropertyReportCreateResponse(
                REPORT_ID,
                ReportStatus.RECEIVED,
                0L
        ));

        mockMvc.perform(post(
                        "/api/property/properties/{propertyId}/reports",
                        PROPERTY_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode": "FALSE_INFO",
                                  "detail": "허위 매물 정보가 포함되어 있어 신고합니다.",
                                  "evidenceFileIds": ["884700000000000003"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId")
                        .value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.version").value(0));

        verify(service).createReport(
                PROPERTY_ID,
                ReportReasonCode.FALSE_INFO,
                DETAIL,
                List.of(FILE_ID),
                actorContext
        );
    }

    @Test
    void createReport_numericEvidenceId_returnsBadRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/property/properties/{propertyId}/reports",
                        PROPERTY_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode": "FALSE_INFO",
                                  "detail": "허위 매물 정보가 포함되어 있어 신고합니다.",
                                  "evidenceFileIds": [884700000000000003]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createReport_endpointRequiresUserOrAgent() throws Exception {
        Method method = PropertyReportController.class.getDeclaredMethod(
                "createReport",
                Long.class,
                PropertyReportCreateRequest.class,
                ActorContext.class
        );

        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/properties/{propertyId}/reports");
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
