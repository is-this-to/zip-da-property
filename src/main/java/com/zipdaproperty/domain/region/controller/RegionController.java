package com.zipdaproperty.domain.region.controller;

import com.zipdaproperty.domain.region.request.RegionSearchRequest;
import com.zipdaproperty.domain.region.response.RegionDetailResponse;
import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import com.zipdaproperty.domain.region.service.RegionService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Region API",
        description = "지역 계층 및 경계 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/regions")
public class RegionController {

    private final RegionService regionService;

    @Operation(
            summary = "최상위 시·도 목록 조회",
            description = """
                    상위 지역이 없는 활성 시·도 목록을 조회합니다.
                    소프트 삭제되거나 비활성화된 지역은 제외하며,
                    지역명 가나다순으로 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/root")
    public ResponseEntity<GlobalResponseDTO<List<RegionSummaryResponse>>> getRootRegions(){
        return ResponseEntity.ok(GlobalResponseDTO.success(regionService.getRootRegions()));
    }

    @Operation(
            summary = "직계 하위 지역 목록 조회",
            description = """
                    전달받은 부모 지역을 기준으로 직계 하위 지역을 조회합니다.
                    활성 상태이며 소프트 삭제되지 않은 지역만 조회하고,
                    지역명 가나다순으로 반환합니다.
                    부모 지역은 존재하지만 자식 지역이 없으면 빈 목록을 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{parentRegionId}/children")
    public ResponseEntity<GlobalResponseDTO<List<RegionSummaryResponse>>> getChildRegions(
            @Parameter(
                    description = "부모 지역 ID",
                    example = "21"
            )
            @Min(
                    value = 1,
                    message = "부모 지역 ID는 1 이상이어야 합니다."
            )
            @PathVariable Long parentRegionId
    ){
        return ResponseEntity.ok(GlobalResponseDTO.success(regionService.getChildRegions(parentRegionId)));
    }

    @Operation(
            summary = "지역명 검색",
            description = """
                    입력한 검색어가 지역명에 포함된 공개 선택 가능 지역을 조회합니다.
                    
                    검색 대상은 시·도, 시·군·구, 읍·면·동이며,
                    RI는 공개 검색 결과에서 제외합니다.
                    
                    활성 상태이며 소프트 삭제되지 않은 지역만 조회하고,
                    지역명 가나다순으로 반환합니다.
                    
                    조회 결과가 없으면 오류가 아니라 빈 목록을 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseDTO<List<RegionSummaryResponse>>> searchRegions(
            @Valid
            @ParameterObject
            @ModelAttribute
            RegionSearchRequest request
    ){
        List<RegionSummaryResponse> response = regionService.searchRegions(request.keyword());

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(
            summary = "지역 상세 및 경계 조회",
            description = """
                    공개 선택 가능한 지역의 기본정보, 대표 좌표,
                    GeoJSON 경계와 지도 표시용 bounds를 조회합니다.
                    
                    공개 선택 대상은 시도, 시군구, 읍면동입니다.
                    RI는 공개 지도 선택에 노출하지 않습니다.
                    
                    zoomLevel에 맞는 단순화 경계를 조회하고,
                    해당 단계가 없으면 원본인 simplificationLevel 0으로
                    fallback합니다.
                    
                    GeoJSON 좌표는 [경도, 위도] 순서입니다.
                    """
    )
    @CustomApiResponse({
        CustomResponseCode.INVALID_REQUEST,
        CustomResponseCode.NOT_FOUND_RESOURCE,
        CustomResponseCode.DB_ERROR,
        CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{regionId}")
    public ResponseEntity<GlobalResponseDTO<RegionDetailResponse>>
            getRegionDetail(
                    @Parameter(
                            description = "지역 ID",
                            example = "21"
                    )
                    @Min(
                            value = 1,
                            message = "지역 ID는 1 이상이어야 합니다."
                    )
                    @PathVariable
                    Long regionId,

                    @Parameter(
                            description = """
                                    현재 Kakao 지도 level입니다.
                                    생략하면 원본 경계인 simplificationLevel 0을 사용합니다.
                                    """,
                            example = "8"
                    )
                    @Min(
                            value = 1,
                            message = "zoomLevel은 1 이상이어야 합니다."
                    )
                    @Max(
                            value = 14,
                            message = "zoomLevel은 14 이하여야 합니다."
                    )
                    @RequestParam(required = false)
                    Integer zoomLevel
    ){
        RegionDetailResponse response =
                regionService.getRegionDetail(
                        regionId,
                        zoomLevel
                );
        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }


}
