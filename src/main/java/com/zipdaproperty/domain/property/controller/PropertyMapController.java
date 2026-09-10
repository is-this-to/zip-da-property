package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.request.PropertyMapBoundsRequest;
import com.zipdaproperty.domain.property.response.PropertyMapBoundsResponse;
import com.zipdaproperty.domain.property.service.PropertyMapBoundsService;
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

@Tag(
        name = "Property Map API",
        description = "지도 영역 기반 공개 매물 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PropertyMapController {

    private final PropertyMapBoundsService
            propertyMapBoundsService;

    @Operation(
            summary = "지도 bounds 기반 공개 매물 조회",
            description = """
                    현재 지도 영역 안에서 공개 가능한 매물을 조회합니다.

                    정확 좌표가 아닌 비식별 공개 좌표인
                    publicLocation만 지도 검색과 응답에 사용합니다.

                    공개 상태가 PUBLISHED이고 거래 상태가
                    AVAILABLE 또는 RESERVED인 매물만 반환합니다.

                    최대 500개까지 반환하고 전체 결과가
                    500개를 초과하면 truncated가 true입니다.

                    조회 결과가 없으면 오류가 아닌 빈 items를 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/map")
    public ResponseEntity<
            GlobalResponseDTO<PropertyMapBoundsResponse>
            >
    getPropertiesInMapBounds(
            @Valid
            @ParameterObject
            @ModelAttribute
            PropertyMapBoundsRequest request
    ) {
        PropertyMapBoundsResponse response =
                propertyMapBoundsService.findProperties(
                        request
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}
