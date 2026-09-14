package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportActionRequest;
import com.zipdaproperty.domain.report.request.PropertyReportAdminListRequest;
import com.zipdaproperty.domain.report.request.PropertyReportAdminAssignmentRequest;
import com.zipdaproperty.domain.report.request.PropertyReportAdminRiskScoreRequest;
import com.zipdaproperty.domain.report.request.PropertyReportAdminStatusChangeRequest;
import com.zipdaproperty.domain.report.response.PropertyReportActionResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminDetailResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListItemResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminAssignmentResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminRiskScoreResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminStatusChangeResponse;
import com.zipdaproperty.domain.report.service.PropertyReportActionService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminDetailService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminListService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminAssignmentService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminRiskScoreService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminStatusChangeService;
import com.zipdaproperty.domain.report.type.ReportActionCode;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private final PropertyReportAdminListService listService =
            mock(PropertyReportAdminListService.class);
    private final PropertyReportAdminDetailService detailService =
            mock(PropertyReportAdminDetailService.class);
    private final PropertyReportAdminStatusChangeService statusChangeService =
            mock(PropertyReportAdminStatusChangeService.class);
    private final PropertyReportAdminAssignmentService assignmentService =
            mock(PropertyReportAdminAssignmentService.class);
    private final PropertyReportAdminRiskScoreService riskScoreService =
            mock(PropertyReportAdminRiskScoreService.class);
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
                        listService,
                        detailService,
                        statusChangeService,
                        assignmentService,
                        riskScoreService,
                        actionService
                );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(actorContextResolver())
                .build();
    }

    @Test
    void getReports_returnsCursorPageWithTsidStrings() throws Exception {
        PropertyReportAdminListRequest request =
                new PropertyReportAdminListRequest(null, 20);
        when(listService.findReports(request, adminContext))
                .thenReturn(new PropertyReportAdminListResponse(
                        List.of(new PropertyReportAdminListItemResponse(
                                REPORT_ID,
                                PROPERTY_ID,
                                1001L,
                                ReportReasonCode.FALSE_INFO,
                                ReportStatus.IN_REVIEW,
                                new BigDecimal("42.50"),
                                3001L,
                                EXECUTED_AT
                        )),
                        "opaque-cursor",
                        true
                ));

        mockMvc.perform(get("/api/admin/property-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reportId")
                        .value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].propertyId")
                        .value(PROPERTY_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].reporterMemberId")
                        .value("1001"))
                .andExpect(jsonPath("$.data.nextCursor")
                        .value("opaque-cursor"))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        verify(listService).findReports(request, adminContext);
    }

    @Test
    void getReport_passesAuditReasonAndReturnsTsidStrings() throws Exception {
        when(detailService.findReport(REPORT_ID, "고객 문의 확인", adminContext))
                .thenReturn(new PropertyReportAdminDetailResponse(
                        REPORT_ID,
                        PROPERTY_ID,
                        1001L,
                        ReportReasonCode.FALSE_INFO,
                        "허위 매물 신고 상세",
                        ReportStatus.IN_REVIEW,
                        new BigDecimal("42.50"),
                        3001L,
                        3L,
                        EXECUTED_AT,
                        List.of()
                ));

        mockMvc.perform(get(
                        "/api/admin/property-reports/{reportId}",
                        REPORT_ID
                ).header("X-Audit-Reason", "고객 문의 확인"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId")
                        .value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.propertyId")
                        .value(PROPERTY_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.version").value(3));

        verify(detailService).findReport(
                REPORT_ID,
                "고객 문의 확인",
                adminContext
        );
    }

    @Test
    void changeStatus_passesVersionAndReturnsIncrementedVersion() throws Exception {
        PropertyReportAdminStatusChangeRequest request =
                new PropertyReportAdminStatusChangeRequest(
                        ReportStatus.TRIAGED,
                        3L
                );
        when(statusChangeService.changeStatus(REPORT_ID, request, adminContext))
                .thenReturn(new PropertyReportAdminStatusChangeResponse(
                        REPORT_ID,
                        ReportStatus.TRIAGED,
                        4L
                ));

        mockMvc.perform(patch(
                        "/api/admin/property-reports/{reportId}/status",
                        REPORT_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "TRIAGED",
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId")
                        .value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("TRIAGED"))
                .andExpect(jsonPath("$.data.version").value(4));

        verify(statusChangeService).changeStatus(
                REPORT_ID,
                request,
                adminContext
        );
    }

    @Test
    void assignAdmin_passesAssigneeAndReturnsIncrementedVersion() throws Exception {
        PropertyReportAdminAssignmentRequest request =
                new PropertyReportAdminAssignmentRequest(3002L, 3L);
        when(assignmentService.assign(REPORT_ID, request, adminContext))
                .thenReturn(new PropertyReportAdminAssignmentResponse(
                        REPORT_ID,
                        3002L,
                        4L
                ));

        mockMvc.perform(patch(
                        "/api/admin/property-reports/{reportId}/assignee",
                        REPORT_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedAdminId": "3002",
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.assignedAdminId").value("3002"))
                .andExpect(jsonPath("$.data.version").value(4));

        verify(assignmentService).assign(REPORT_ID, request, adminContext);
    }

    @Test
    void changeRiskScore_passesScoreAndReturnsIncrementedVersion() throws Exception {
        PropertyReportAdminRiskScoreRequest request =
                new PropertyReportAdminRiskScoreRequest(new BigDecimal("42.50"), 3L);
        when(riskScoreService.changeRiskScore(REPORT_ID, request, adminContext))
                .thenReturn(new PropertyReportAdminRiskScoreResponse(
                        REPORT_ID,
                        new BigDecimal("42.50"),
                        4L
                ));

        mockMvc.perform(patch(
                        "/api/admin/property-reports/{reportId}/risk-score",
                        REPORT_ID
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "riskScore": 42.50,
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.data.riskScore").value(42.50))
                .andExpect(jsonPath("$.data.version").value(4));

        verify(riskScoreService).changeRiskScore(REPORT_ID, request, adminContext);
    }

    @Test
    void reportAdminUpdateEndpoints_requireCsOrSuperAdmin() throws Exception {
        Method assignmentMethod = PropertyReportAdminController.class.getDeclaredMethod(
                "assignAdmin",
                Long.class,
                PropertyReportAdminAssignmentRequest.class,
                ActorContext.class
        );
        Method riskScoreMethod = PropertyReportAdminController.class.getDeclaredMethod(
                "changeRiskScore",
                Long.class,
                PropertyReportAdminRiskScoreRequest.class,
                ActorContext.class
        );

        assertThat(assignmentMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{reportId}/assignee");
        assertThat(riskScoreMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{reportId}/risk-score");
        assertThat(List.of(assignmentMethod, riskScoreMethod))
                .allSatisfy(method -> assertThat(
                        method.getAnnotation(PreAuthorize.class).value()
                ).isEqualTo("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')"));
    }

    @Test
    void reportAdminEndpoints_requireCsOrSuperAdmin() throws Exception {
        RequestMapping requestMapping = PropertyReportAdminController.class
                .getAnnotation(RequestMapping.class);
        Method listMethod = PropertyReportAdminController.class.getDeclaredMethod(
                "getReports",
                PropertyReportAdminListRequest.class,
                ActorContext.class
        );
        Method detailMethod = PropertyReportAdminController.class.getDeclaredMethod(
                "getReport",
                Long.class,
                String.class,
                ActorContext.class
        );
        Method statusMethod = PropertyReportAdminController.class.getDeclaredMethod(
                "changeStatus",
                Long.class,
                PropertyReportAdminStatusChangeRequest.class,
                ActorContext.class
        );

        assertThat(requestMapping.value())
                .containsExactly("/api/admin/property-reports");
        assertThat(listMethod.getAnnotation(GetMapping.class)).isNotNull();
        assertThat(detailMethod.getAnnotation(GetMapping.class).value())
                .containsExactly("/{reportId}");
        assertThat(statusMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{reportId}/status");
        assertThat(List.of(listMethod, detailMethod, statusMethod))
                .allSatisfy(method -> assertThat(
                        method.getAnnotation(PreAuthorize.class).value()
                ).isEqualTo("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')"));
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
