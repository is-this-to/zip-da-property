package com.zipdaproperty.domain.location.controller;

import com.zipdaproperty.domain.location.request.KakaoAddressSearchRequest;
import com.zipdaproperty.domain.location.response.KakaoAddressSearchResponse;
import com.zipdaproperty.domain.location.service.KakaoAddressSearchService;
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
}
