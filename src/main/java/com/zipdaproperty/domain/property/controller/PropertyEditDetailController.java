package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.domain.property.service.PropertyEditDetailService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Property API",
        description = "매물 조회·등록·수정·상태 변경·삭제·복구 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PropertyEditDetailController {

    private final PropertyEditDetailService
            propertyEditDetailService;

    @Operation(
            summary = "매물 수정용 상세 조회",
            description = """
                    매물 작성자 또는 허용된 관리자가
                    수정 화면에 필요한 현재 매물 정보를 조회합니다.
                    소프트 삭제된 매물은 조회하지 않습니다.
                    응답의 version은 이후 매물 수정 요청의
                    If-Match 헤더와 요청 본문에 사용합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize(
            "hasAnyRole("
                    + "'USER', "
                    + "'AGENT', "
                    + "'CS_ADMIN', "
                    + "'SUPER_ADMIN'"
                    + ")"
    )
    @GetMapping("/{propertyId}/edit")
    public ResponseEntity<
            GlobalResponseDTO<PropertyEditDetailResponse>
            >
    getPropertyEditDetail(
            @Parameter(
                    description = "수정용 상세 정보를 조회할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyEditDetailResponse response =
                propertyEditDetailService.getEditDetail(
                        propertyId,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}