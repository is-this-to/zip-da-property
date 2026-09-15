package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.request.PropertyMyListRequest;
import com.zipdaproperty.domain.property.response.PropertyMyListResponse;
import com.zipdaproperty.domain.property.service.PropertyMyListService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "My Property API",
        description = "로그인 회원의 매물 관리 API"
)
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/properties")
public class PropertyMyController {

    private final PropertyMyListService
            propertyMyListService;

    @Operation(
            summary = "내가 등록한 매물 목록 조회",
            description = """
                    로그인한 일반 회원 또는 중개사가
                    자신이 등록한 활성 매물 목록을 조회합니다.
                    소프트 삭제된 매물은 조회 결과에서 제외합니다.
                    목록은 updatedAt과 propertyId를 기준으로 정렬하며
                    서버가 발급한 cursor를 사용해 다음 페이지를 조회합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.METHOD_NOT_ALLOWED,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping
    public ResponseEntity<
            GlobalResponseDTO<PropertyMyListResponse>
            >
    getMyProperties(
            @Valid
            @ModelAttribute
            PropertyMyListRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyMyListResponse response =
                propertyMyListService.findMyProperties(
                        request,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }
}