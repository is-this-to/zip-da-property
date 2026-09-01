package com.zipdaproperty.domain.region.controller;

import com.zipdaproperty.domain.region.response.RegionSummaryResponse;
import com.zipdaproperty.domain.region.service.RegionService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Region API",
        description = "지역 계층 및 경계 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
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
}
