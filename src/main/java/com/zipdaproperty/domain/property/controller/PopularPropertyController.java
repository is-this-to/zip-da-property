package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.constant.PopularPropertyRegion;
import com.zipdaproperty.domain.property.response.PopularPropertyItemResponse;
import com.zipdaproperty.domain.property.service.PopularPropertyService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PopularPropertyController {

    private final PopularPropertyService popularPropertyService;

    @Operation(summary = "찜 많은 인기 매물 조회", description = "전국 또는 특별·광역시 단위로 활성 찜 수가 많은 공개 매물을 조회합니다.")
    @CustomApiResponse({
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/popular")
    public ResponseEntity<GlobalResponseDTO<List<PopularPropertyItemResponse>>> findPopularProperties(
            @RequestParam(defaultValue = "ALL") PopularPropertyRegion region,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 10, message = "size는 10 이하여야 합니다.")
            int size
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                popularPropertyService.findPopularProperties(region, size)
        ));
    }
}
