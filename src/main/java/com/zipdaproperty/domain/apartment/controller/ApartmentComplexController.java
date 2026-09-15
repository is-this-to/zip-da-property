package com.zipdaproperty.domain.apartment.controller;

import com.zipdaproperty.domain.apartment.request.ApartmentComplexSearchRequest;
import com.zipdaproperty.domain.apartment.response.ApartmentComplexSearchResponse;
import com.zipdaproperty.domain.apartment.service.ApartmentComplexQueryService;
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

@Tag(name = "Apartment Complex API", description = "매물 등록용 공동주택 단지 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/apartment-complexes")
public class ApartmentComplexController {

    private final ApartmentComplexQueryService apartmentComplexQueryService;

    @Operation(
            summary = "아파트 단지 검색",
            description = """
                    검증된 Region과 선택 검색어를 기준으로 활성 아파트 단지를 조회합니다.
                    검색어가 없으면 해당 Region의 활성 단지를 단지명 순으로 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<ApartmentComplexSearchResponse>>
            searchApartmentComplexes(
                    @Valid @ParameterObject @ModelAttribute ApartmentComplexSearchRequest request
    ) {
        return ResponseEntity.ok(
                GlobalResponseDTO.success(apartmentComplexQueryService.search(request))
        );
    }
}
