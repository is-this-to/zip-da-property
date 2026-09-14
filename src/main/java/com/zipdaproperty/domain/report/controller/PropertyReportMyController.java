package com.zipdaproperty.domain.report.controller;

import com.zipdaproperty.domain.report.request.PropertyReportMyListRequest;
import com.zipdaproperty.domain.report.response.PropertyReportMyListResponse;
import com.zipdaproperty.domain.report.service.PropertyReportMyListService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/me/property-reports")
public class PropertyReportMyController {

    private final PropertyReportMyListService propertyReportMyListService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    public ResponseEntity<GlobalResponseDTO<PropertyReportMyListResponse>> getMyReports(
            @Valid @ModelAttribute PropertyReportMyListRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        PropertyReportMyListResponse response =
                propertyReportMyListService.findMyReports(request, actorContext);

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }
}
