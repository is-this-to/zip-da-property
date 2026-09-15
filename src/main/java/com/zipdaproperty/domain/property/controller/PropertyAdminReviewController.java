package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.request.PropertyPublicationAdminListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminDetailResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListResponse;
import com.zipdaproperty.domain.property.service.PropertyPublicationAdminDetailService;
import com.zipdaproperty.domain.property.service.PropertyPublicationAdminListService;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationAdminListRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminDetailResponse;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminListResponse;
import com.zipdaproperty.domain.property.verification.service.PropertyVerificationAdminDetailService;
import com.zipdaproperty.domain.property.verification.service.PropertyVerificationAdminListService;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class PropertyAdminReviewController {
    private final PropertyVerificationAdminListService verificationListService;
    private final PropertyVerificationAdminDetailService verificationDetailService;
    private final PropertyPublicationAdminListService publicationListService;
    private final PropertyPublicationAdminDetailService publicationDetailService;

    @GetMapping("/property-verifications")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationAdminListResponse>> verifications(
            @Valid @ModelAttribute PropertyVerificationAdminListRequest request,
            @Parameter(hidden = true) ActorContext actor
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(verificationListService.find(request, actor)));
    }

    @GetMapping("/property-verifications/{verificationId}")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationAdminDetailResponse>> verification(
            @PathVariable @Positive Long verificationId,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReason,
            @Parameter(hidden = true) ActorContext actor
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                verificationDetailService.find(verificationId, auditReason, actor)));
    }

    @GetMapping("/property-publication-reviews")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PropertyPublicationAdminListResponse>> publications(
            @Valid @ModelAttribute PropertyPublicationAdminListRequest request,
            @Parameter(hidden = true) ActorContext actor
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(publicationListService.find(request, actor)));
    }

    @GetMapping("/property-publication-reviews/{propertyId}")
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PropertyPublicationAdminDetailResponse>> publication(
            @PathVariable @Positive Long propertyId,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReason,
            @Parameter(hidden = true) ActorContext actor
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                publicationDetailService.find(propertyId, auditReason, actor)));
    }
}
