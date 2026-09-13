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
            summary = "지도 확대 단계별 공개 매물 조회",
            description = """
                    현재 지도 bounds와 Kakao ROADMAP zoomLevel을 기준으로
                    공개 가능한 매물의 지도 표시 데이터를 조회합니다.

                    zoomLevel 13~14는 시·도 집계,
                    zoomLevel 11~12는 시·군·구 집계,
                    zoomLevel 8~10은 읍·면·동 집계를 반환합니다.

                    zoomLevel 5~7은 프런트 Kakao MarkerClusterer에 전달할
                    PROPERTY_POINTS를 반환합니다.

                    zoomLevel 1~4는 개별 가격 마커에 사용할
                    PROPERTY_MARKER를 반환합니다.

                    지도 검색과 응답에는 정확 좌표가 아닌
                    비식별 공개 좌표 publicLocation만 사용합니다.

                    개별 좌표는 최대 500개까지 반환하며,
                    전체 결과가 500개를 초과하면 truncated가 true입니다.

                    매물 유형, 거래 유형, 거래 유형별 가격, 관리비,
                    전용면적, 방 개수, 등록 주체, 사용승인일 및
                    주차·엘리베이터·반려동물 조건을 선택적으로 적용합니다.

                    정렬은 LATEST, PRICE_ASC, PRICE_DESC, AREA_DESC를
                    지원하며 지역 집계가 아닌 개별 매물 응답에 적용합니다.

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
