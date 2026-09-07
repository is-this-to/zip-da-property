package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.idempotency.service.PropertyIdempotencyService;
import com.zipdaproperty.domain.property.request.PropertyCreateRequest;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.response.GlobalResponseDTO;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Property API",
        description = "매물 등록·수정·상태 변경 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PropertyController {

    private final PropertyIdempotencyService propertyIdempotencyService;

    @Operation(
            summary = "매물 등록",
            description = """
                    로그인한 일반 회원 또는 중개사가 매물을 등록합니다.
                    일반 회원은 집주인 직접 등록만 가능하고,
                    중개사는 중개사 매물 등록만 가능합니다.
                    등록된 매물은 공개 검수 대기 상태로 생성됩니다.
                    동일한 Idempotency-Key로 동일 요청을 반복하면
                    최초 요청의 응답을 재사용합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.INVALID_PRICE_COMBINATION,
            CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
            CustomResponseCode.IDEMPOTENCY_KEY_REQUIRED,
            CustomResponseCode.IDEMPOTENCY_CONFLICT,
            CustomResponseCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<PropertyCreateResponse>> createProperty(
            @Parameter(
                    description = "매물 등록 요청의 중복 처리를 방지하는 고유 키",
                    required = true,
                    example = "property-create-1001"
            )
            @RequestHeader(
                    name = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,

            @Valid
            @RequestBody
            PropertyCreateRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        PropertyCreateResponse response =
                propertyIdempotencyService.create(
                        idempotencyKey,
                        request.toCommand(),
                        actorContext
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(response));
    }
}