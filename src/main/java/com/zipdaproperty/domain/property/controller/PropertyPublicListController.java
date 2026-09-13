package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.request.PropertyPublicListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicListResponse;
import com.zipdaproperty.domain.property.service.PropertyPublicListService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Property Public List API", description = "공개 매물 목록 커서 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PropertyPublicListController {

    private final PropertyPublicListService propertyPublicListService;

    @Operation(
            summary = "지도 영역의 공개 매물 목록 조회",
            description = "bounds와 검색 조건을 기준으로 공개 매물을 커서 방식으로 조회합니다."
    )
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<PropertyPublicListResponse>> findProperties(
            @Valid @ParameterObject @ModelAttribute PropertyPublicListRequest request
    ) {
        return ResponseEntity.ok(
                GlobalResponseDTO.success(propertyPublicListService.findProperties(request))
        );
    }
}
