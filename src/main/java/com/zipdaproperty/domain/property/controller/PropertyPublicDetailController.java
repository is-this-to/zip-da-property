package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.response.PropertyPublicDetailResponse;
import com.zipdaproperty.domain.property.service.PropertyPublicDetailService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.constant.InternalHeaderName;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
public class PropertyPublicDetailController {

    private final PropertyPublicDetailService propertyPublicDetailService;

    @Operation(
            summary = "공개 매물 상세 조회",
            description = "공개 가능한 매물의 상세 정보와 공개 위치만 조회합니다."
    )
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{propertyId}")
    public ResponseEntity<GlobalResponseDTO<PropertyPublicDetailResponse>> getPublicDetail(
            @PathVariable
            @Positive(message = "매물 ID는 0보다 커야 합니다.")
            Long propertyId,
            @RequestHeader(
                    value = InternalHeaderName.X_USER_ID,
                    required = false
            )
            @Positive(message = "회원 ID는 0보다 커야 합니다.")
            Long memberId
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                propertyPublicDetailService.findDetail(propertyId, memberId)
        ));
    }
}
