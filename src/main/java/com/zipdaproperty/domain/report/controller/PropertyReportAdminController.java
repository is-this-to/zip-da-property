package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAdminListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAdminDetailResponse;
import com.zipdaproperty.domain.report.response.PropertyReportAdminListResponse;
import com.zipdaproperty.domain.report.service.PropertyReportAdminDetailService;
import com.zipdaproperty.domain.report.service.PropertyReportAdminListService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/property-reports")
public class PropertyReportAdminController {

    private final PropertyReportAdminListService propertyReportAdminListService;
    private final PropertyReportAdminDetailService propertyReportAdminDetailService;

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
}
