package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAppealCreateRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAppealCreateResponse;
import com.zipdaproperty.domain.report.service.PropertyReportAppealService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property-reports")
public class PropertyReportAppealController {

    private final PropertyReportAppealService propertyReportAppealService;

    @PostMapping("/{reportId}/appeals")
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportAppealCreateResponse>> createAppeal(
            @PathVariable
            @Positive(message = "신고 ID는 0보다 커야 합니다.")
            Long reportId,
            @Valid @RequestBody PropertyReportAppealCreateRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        PropertyReportAppealCreateResponse response =
                propertyReportAppealService.createAppeal(
                        reportId,
                        request.detail(),
                        actorContext
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(response));
    }
}
