package com.zipdaproperty.domain.property.verification.controller;

import com.zipdaproperty.domain.property.verification.request.PropertyVerificationReviewRequest;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationSubmitRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationResponse;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.service.PropertyVerificationService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Tag(name = "Property Verification API", description = "매물 소유·중개 검증 신청 및 관리자 검토 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties/{propertyId}")
public class PropertyVerificationController {

    private final PropertyVerificationService propertyVerificationService;

    @Operation(summary = "매물 검증 신청")
    @CustomApiResponse(
            value = {
                    CustomResponseCode.INVALID_REQUEST,
                    CustomResponseCode.VERSION_CONFLICT,
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID,
                    CustomResponseCode.FILE_OWNERSHIP_REQUIRED
            },
            successResponseCode = "201",
            successDescription = "CREATED"
    )
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/verifications")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationResponse>> submit(
            @PathVariable Long propertyId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PropertyVerificationSubmitRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        validateVersionAgreement(parseIfMatch(ifMatch), request.version());
        PropertyVerificationResponse response = propertyVerificationService
                .submit(propertyId, request, actorContext);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "집주인 매물 검증 신청")
    @CustomApiResponse(
            value = {
                    CustomResponseCode.INVALID_REQUEST,
                    CustomResponseCode.VERSION_CONFLICT,
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID,
                    CustomResponseCode.FILE_OWNERSHIP_REQUIRED
            },
            successResponseCode = "202",
            successDescription = "ACCEPTED"
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/verifications/owner")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationResponse>>
    submitOwner(
            @PathVariable Long propertyId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PropertyVerificationSubmitRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        validateVersionAgreement(parseIfMatch(ifMatch), request.version());
        PropertyVerificationResponse response = propertyVerificationService
                .submitForType(
                        propertyId,
                        PropertyVerificationType.OWNER,
                        request,
                        actorContext
                );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "세입자 매물 검증 신청")
    @CustomApiResponse(
            value = {
                    CustomResponseCode.INVALID_REQUEST,
                    CustomResponseCode.VERSION_CONFLICT,
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID,
                    CustomResponseCode.FILE_OWNERSHIP_REQUIRED
            },
            successResponseCode = "202",
            successDescription = "ACCEPTED"
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/verifications/tenant")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationResponse>>
    submitTenant(
            @PathVariable Long propertyId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PropertyVerificationSubmitRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        validateVersionAgreement(parseIfMatch(ifMatch), request.version());
        PropertyVerificationResponse response = propertyVerificationService
                .submitForType(
                        propertyId,
                        PropertyVerificationType.TENANT,
                        request,
                        actorContext
                );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "매물 재인증 신청")
    @CustomApiResponse(
            value = {
                    CustomResponseCode.INVALID_REQUEST,
                    CustomResponseCode.VERSION_CONFLICT,
                    CustomResponseCode.PROPERTY_NOT_FOUND,
                    CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
                    CustomResponseCode.PROPERTY_VERIFICATION_ALREADY_IN_PROGRESS,
                    CustomResponseCode.PROPERTY_VERIFICATION_EVIDENCE_INVALID,
                    CustomResponseCode.FILE_OWNERSHIP_REQUIRED
            },
            successResponseCode = "202",
            successDescription = "ACCEPTED"
    )
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/reverification")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationResponse>>
    resubmit(
            @PathVariable Long propertyId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PropertyVerificationSubmitRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        validateVersionAgreement(parseIfMatch(ifMatch), request.version());
        PropertyVerificationResponse response = propertyVerificationService
                .resubmit(propertyId, request, actorContext);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GlobalResponseDTO.success(response));
    }

    @Operation(summary = "매물 검증 승인 또는 반려")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.PROPERTY_VERIFICATION_NOT_FOUND,
            CustomResponseCode.PROPERTY_VERIFICATION_REVIEW_NOT_ALLOWED
    })
    @PreAuthorize("hasAnyRole('CS_ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/verifications/{verificationId}")
    public ResponseEntity<GlobalResponseDTO<PropertyVerificationResponse>> review(
            @PathVariable Long propertyId,
            @PathVariable Long verificationId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PropertyVerificationReviewRequest request,
            @Parameter(hidden = true) ActorContext actorContext
    ) {
        validateVersionAgreement(parseIfMatch(ifMatch), request.version());
        return ResponseEntity.ok(GlobalResponseDTO.success(
                propertyVerificationService.review(propertyId, verificationId, request, actorContext)
        ));
    }

    private Long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "If-Match 헤더는 필수입니다.");
        }
        String value = ifMatch.trim();
        boolean startsWithQuote = value.startsWith("\"");
        boolean endsWithQuote = value.endsWith("\"");
        if (startsWithQuote != endsWithQuote) {
            throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "If-Match 헤더의 따옴표 형식이 올바르지 않습니다.");
        }
        if (startsWithQuote) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            Long version = Long.valueOf(value);
            if (version < 0) {
                throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "If-Match version은 0 이상이어야 합니다.");
            }
            return version;
        } catch (NumberFormatException exception) {
            throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "If-Match 헤더에는 숫자 version을 입력해야 합니다.");
        }
    }

    private void validateVersionAgreement(Long headerVersion, Long bodyVersion) {
        if (!Objects.equals(headerVersion, bodyVersion)) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "If-Match version과 요청 본문의 version이 일치하지 않습니다."
            );
        }
    }
}
