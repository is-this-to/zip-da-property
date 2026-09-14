package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAdminListRequest;
import com.zipdaproperty.domain.report.request.PropertyReportAdminStatusChangeRequest;
import com.zipdaproperty.domain.report.request.PropertyReportActionRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminDetailResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminStatusChangeResponse;
import com.zipdaproperty.domain.report.response.PropertyReportActionResponse;
import com.zipdaproperty.domain.report.service.PropertyReportActionService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminDetailService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminListService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminStatusChangeService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/property-reports")
public class PropertyReportAdminController {

    private final PropertyReportAdminListService propertyReportAdminListService;
    private final PropertyReportAdminDetailService propertyReportAdminDetailService;
    private final PropertyReportAdminStatusChangeService propertyReportAdminStatusChangeService;
    private final PropertyReportActionService propertyReportActionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportAdminListResponse>> getReports(
            @Valid @ModelAttribute PropertyReportAdminListRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        PropertyReportAdminListResponse response =
                propertyReportAdminListService.findReports(request, actorContext);

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.AUDIT_REASON_REQUIRED,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportAdminDetailResponse>> getReport(
            @PathVariable
            @Positive(message = "신고 ID는 0보다 커야 합니다.")
            Long reportId,
            @RequestHeader(value = "X-Audit-Reason", required = false)
            String auditReason,
            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyReportAdminDetailResponse response =
                propertyReportAdminDetailService.findReport(
                        reportId,
                        auditReason,
                        actorContext
                );

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.INVALID_REPORT_TRANSITION,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportAdminStatusChangeResponse>> changeStatus(
            @PathVariable
            @Positive(message = "신고 ID는 0보다 커야 합니다.")
            Long reportId,
            @Valid @RequestBody
            PropertyReportAdminStatusChangeRequest request,
            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyReportAdminStatusChangeResponse response =
                propertyReportAdminStatusChangeService.changeStatus(
                        reportId,
                        request,
                        actorContext
                );

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @PostMapping("/{reportId}/actions")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.INVALID_STATUS_TRANSITION,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.MEMBER_API_UNAVAILABLE,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportActionResponse>> createAction(
            @PathVariable
            @Positive(message = "신고 ID는 0보다 커야 합니다.")
            Long reportId,
            @Valid @RequestBody
            PropertyReportActionRequest request,
            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyReportActionResponse response =
                propertyReportActionService.executeAction(
                        reportId,
                        request,
                        actorContext
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(response));
    }
}
