package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportAppealAdminReviewRequest;
import com.zipdaproperty.domain.report.response.PropertyReportAppealAdminReviewResponse;
import com.zipdaproperty.domain.report.service.PropertyReportAppealAdminReviewService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/property-report-appeals")
public class PropertyReportAppealAdminController {

    private final PropertyReportAppealAdminReviewService reviewService;

    @PatchMapping("/{appealId}")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.APPEAL_NOT_ALLOWED,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportAppealAdminReviewResponse>> review(
            @PathVariable
            @Positive(message = "이의신청 ID는 0보다 커야 합니다.")
            Long appealId,
            @Valid @RequestBody PropertyReportAppealAdminReviewRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        PropertyReportAppealAdminReviewResponse response = reviewService.review(
                appealId,
                request,
                actorContext
        );
        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }
}
