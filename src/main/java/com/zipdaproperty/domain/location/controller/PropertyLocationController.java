package com.zipdaproperty.domain.location.controller;

import com.zipdaproperty.domain.location.request.KakaoAddressSearchRequest;
import com.zipdaproperty.domain.location.request.PropertyLocationValidationRequest;
import com.zipdaproperty.domain.location.response.KakaoAddressSearchResponse;
import com.zipdaproperty.domain.location.response.PropertyLocationValidationResponse;
import com.zipdaproperty.domain.location.service.KakaoAddressSearchService;
import com.zipdaproperty.domain.location.service.PropertyLocationValidationFacade;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Property Location API",
        description = "매물 주소 검색 및 위치 변환 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/locations")
public class PropertyLocationController {

    private final KakaoAddressSearchService
            kakaoAddressSearchService;

    private final PropertyLocationValidationFacade
            propertyLocationValidationFacade;

    @Operation(
            summary = "카카오 주소 검색",
            description = """
                    도로명 또는 지번 주소를 검색하여 도로명 주소, 지번 주소,
                    법정동 코드, 건물명, 경도와 위도를 반환합니다.
                    일반 회원 또는 중개사 권한이 필요합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.KAKAO_LOCAL_API_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping("/kakao-address")
    public ResponseEntity<
            GlobalResponseDTO<KakaoAddressSearchResponse>
            > searchKakaoAddress(
            @Valid
            @ParameterObject
            @ModelAttribute
            KakaoAddressSearchRequest request
    ) {
        KakaoAddressSearchResponse response =
                kakaoAddressSearchService.search(request);

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @Operation(
            summary = "매물 주소와 Region 검증",
            description = """
                    카카오 주소 검색에서 선택한 법정동 코드와 좌표를 내부 활성 Region 및
                    원본 경계와 대조하고, 매물 등록에 사용할 Region ID를 반환합니다.
                    이 API는 검증만 수행하며 주소 데이터를 저장하지 않습니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.PROPERTY_REGION_NOT_FOUND,
            CustomResponseCode.PROPERTY_REGION_BOUNDARY_NOT_FOUND,
            CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/validate")
    public ResponseEntity<GlobalResponseDTO<PropertyLocationValidationResponse>>
            validatePropertyLocation(
                    @Valid @RequestBody PropertyLocationValidationRequest request
    ) {
        return ResponseEntity.ok(
                GlobalResponseDTO.success(
                        propertyLocationValidationFacade.validate(request)
                )
        );
    }
}
