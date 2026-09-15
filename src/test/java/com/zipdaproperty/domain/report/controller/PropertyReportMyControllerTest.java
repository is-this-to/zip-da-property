package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportMyListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportMyListResponse;
import com.zipdaproperty.domain.report.service.PropertyReportMyListService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyReportMyControllerTest {

    private final PropertyReportMyListService service =
            mock(PropertyReportMyListService.class);
    private final ActorContext actorContext = ActorContext.member(
            1001L,
            ActorRole.USER,
            "property-report-my-controller-test"
    );

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PropertyReportMyController(service))
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void getMyReports_propertyApiPath_usesDefaultPageSize() throws Exception {
        PropertyReportMyListRequest request =
                new PropertyReportMyListRequest(null, 20);
        when(service.findMyReports(request, actorContext))
                .thenReturn(new PropertyReportMyListResponse(
                        List.of(),
                        null,
                        false
                ));

        mockMvc.perform(get("/api/property/me/property-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(service).findMyReports(request, actorContext);
    }

    @Test
    void getMyReports_endpointRequiresUserOrAgent() throws Exception {
        RequestMapping requestMapping = PropertyReportMyController.class
                .getAnnotation(RequestMapping.class);
        Method method = PropertyReportMyController.class.getDeclaredMethod(
                "getMyReports",
                PropertyReportMyListRequest.class,
                ActorContext.class
        );

        assertThat(requestMapping.value())
                .containsExactly("/api/property/me/property-reports");
        assertThat(method.getAnnotation(GetMapping.class)).isNotNull();
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
