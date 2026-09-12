package com.zipdaproperty.domain.property.controller;

import com.zipdaproperty.domain.property.idempotency.service.PropertyIdempotencyService;
import com.zipdaproperty.domain.property.request.PropertyCreateRequest;
import com.zipdaproperty.domain.property.request.PropertyDeleteRequest;
import com.zipdaproperty.domain.property.request.PropertyPublicationStatusChangeRequest;
import com.zipdaproperty.domain.property.request.PropertyRestoreRequest;
import com.zipdaproperty.domain.property.request.PropertyTransactionStatusChangeRequest;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.domain.property.response.PropertyCreateResponse;
import com.zipdaproperty.domain.property.response.PropertyPublicationStatusChangeResponse;
import com.zipdaproperty.domain.property.response.PropertyRestoreResponse;
import com.zipdaproperty.domain.property.response.PropertyTransactionStatusChangeResponse;
import com.zipdaproperty.domain.property.response.PropertyUpdateResponse;
import com.zipdaproperty.domain.property.service.PropertyDeleteService;
import com.zipdaproperty.domain.property.service.PropertyPublicationStatusChangeService;
import com.zipdaproperty.domain.property.service.PropertyRestoreService;
import com.zipdaproperty.domain.property.service.PropertyTransactionStatusChangeService;
import com.zipdaproperty.domain.property.service.PropertyUpdateService;
import com.zipdaproperty.global.config.openapi.CustomApiResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Tag(
        name = "Property API",
        description = "매물 등록·수정·상태 변경·삭제·복구 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/property/properties")
public class PropertyController {

    private final PropertyIdempotencyService
            propertyIdempotencyService;

    private final PropertyUpdateService propertyUpdateService;

    private final PropertyTransactionStatusChangeService
            propertyTransactionStatusChangeService;

    private final PropertyPublicationStatusChangeService
            propertyPublicationStatusChangeService;

    private final PropertyDeleteService propertyDeleteService;

    private final PropertyRestoreService propertyRestoreService;

    @Operation(
            summary = "매물 등록",
            description = """
                    로그인한 일반 회원 또는 중개사가 매물을 등록합니다.
                    일반 회원은 집주인 직접 등록만 가능하고,
                    중개사는 중개사 매물 등록만 가능합니다.
                    등록된 매물은 공개 검수 대기 상태로 생성됩니다.
                    카카오 주소 결과의 법정동 코드와 좌표를 검증하고,
                    정확 위치와 지도 공개용 비식별 위치를 함께 저장합니다.
                    동일한 Idempotency-Key로 동일 요청을 반복하면
                    최초 요청의 응답을 재사용합니다.
                    저장 전에 주소·가격·등록자·이미지 checksum과
                    본문 위험 패턴을 검사합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.INVALID_PRICE_COMBINATION,
            CustomResponseCode.PROPERTY_CREATE_NOT_ALLOWED,
            CustomResponseCode.PROPERTY_REGION_NOT_FOUND,
            CustomResponseCode.PROPERTY_REGION_BOUNDARY_NOT_FOUND,
            CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH,
            CustomResponseCode.PROPERTY_PUBLIC_LOCATION_GENERATION_FAILED,
            CustomResponseCode.PROPERTY_REGISTRATION_RISK_BLOCKED,
            CustomResponseCode.PROPERTY_DUPLICATE_DETECTED,
            CustomResponseCode.IDEMPOTENCY_KEY_REQUIRED,
            CustomResponseCode.IDEMPOTENCY_CONFLICT,
            CustomResponseCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.DB_DUPLICATED_KEY_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<PropertyCreateResponse>>
    createProperty(
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

    @Operation(
            summary = "매물 수정",
            description = """
                    매물 작성자 또는 허용된 관리자가
                    매물의 핵심 정보를 부분 수정합니다.
                    If-Match 헤더와 요청 본문의 version은
                    반드시 동일해야 합니다.
                    DB의 현재 version이 요청 version과 다르면
                    수정하지 않고 VERSION_CONFLICT를 반환합니다.
                    address를 전달한 경우에만 정확 주소와 공개 위치를
                    다시 검증하고 변경합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.NOT_FOUND_RESOURCE,
            CustomResponseCode.INVALID_PRICE_COMBINATION,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.PROPERTY_OWNERSHIP_REQUIRED,
            CustomResponseCode.PROPERTY_REGION_NOT_FOUND,
            CustomResponseCode.PROPERTY_REGION_BOUNDARY_NOT_FOUND,
            CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH,
            CustomResponseCode.PROPERTY_PUBLIC_LOCATION_GENERATION_FAILED,
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
    @PatchMapping("/{propertyId}")
    public ResponseEntity<GlobalResponseDTO<PropertyUpdateResponse>>
    updateProperty(
            @Parameter(
                    description = "수정할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(
                    description = "마지막으로 조회한 매물 version",
                    required = true,
                    example = "\"0\""
            )
            @RequestHeader(
                    name = "If-Match",
                    required = false
            )
            String ifMatch,

            @Valid
            @RequestBody
            PropertyUpdateRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        Long ifMatchVersion = parseIfMatch(ifMatch);

        validateVersionAgreement(
                ifMatchVersion,
                request.version()
        );

        PropertyUpdateResponse response =
                propertyUpdateService.update(
                        propertyId,
                        request,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @Operation(
            summary = "매물 거래 상태 변경",
            description = """
                    매물 작성자 또는 허용된 관리자가
                    매물의 거래 상태를 변경합니다.
                    If-Match 헤더와 요청 본문의 version은
                    반드시 동일해야 합니다.
                    AVAILABLE, RESERVED, COMPLETED 사이의
                    허용된 상태 전이만 처리합니다.
                    상태 변경 결과는 전체 revision과
                    상태 전용 history에 함께 기록합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.INVALID_STATUS_TRANSITION,
            CustomResponseCode.VERSION_CONFLICT,
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
    @PatchMapping("/{propertyId}/transaction-status")
    public ResponseEntity<
            GlobalResponseDTO<
                    PropertyTransactionStatusChangeResponse
                    >
            >
    changeTransactionStatus(
            @Parameter(
                    description = "거래 상태를 변경할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(
                    description = "마지막으로 조회한 매물 version",
                    required = true,
                    example = "\"1\""
            )
            @RequestHeader(
                    name = "If-Match",
                    required = false
            )
            String ifMatch,

            @Valid
            @RequestBody
            PropertyTransactionStatusChangeRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        Long ifMatchVersion = parseIfMatch(ifMatch);

        validateVersionAgreement(
                ifMatchVersion,
                request.version()
        );

        PropertyTransactionStatusChangeResponse response =
                propertyTransactionStatusChangeService.change(
                        propertyId,
                        request,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @Operation(
            summary = "매물 공개 상태 변경",
            description = """
                    매물의 검수 승인·거절·재신청·숨김·재공개를 처리합니다.
                    검수 승인과 거절은 관리자만 수행할 수 있고,
                    거절된 매물의 재신청은 작성자만 수행할 수 있습니다.
                    숨김과 재공개는 작성자 또는 허용된 관리자가 수행합니다.
                    재공개는 거래 상태가 AVAILABLE인 경우에만 허용됩니다.
                    If-Match 헤더와 요청 본문의 version은
                    반드시 동일해야 합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.INVALID_STATUS_TRANSITION,
            CustomResponseCode.VERSION_CONFLICT,
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
    @PatchMapping("/{propertyId}/publication-status")
    public ResponseEntity<
            GlobalResponseDTO<
                    PropertyPublicationStatusChangeResponse
                    >
            >
    changePublicationStatus(
            @Parameter(
                    description = "공개 상태를 변경할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(
                    description = "마지막으로 조회한 매물 version",
                    required = true,
                    example = "\"1\""
            )
            @RequestHeader(
                    name = "If-Match",
                    required = false
            )
            String ifMatch,

            @Valid
            @RequestBody
            PropertyPublicationStatusChangeRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        Long ifMatchVersion = parseIfMatch(ifMatch);

        validateVersionAgreement(
                ifMatchVersion,
                request.version()
        );

        PropertyPublicationStatusChangeResponse response =
                propertyPublicationStatusChangeService.change(
                        propertyId,
                        request,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @Operation(
            summary = "매물 소프트 삭제",
            description = """
                    매물 작성자 또는 허용된 관리자가
                    활성 상태의 매물을 소프트 삭제합니다.
                    실제 DB 행을 제거하지 않고 deletedAt,
                    deletedByMemberId, deletedByRole,
                    deleteReason을 기록합니다.
                    If-Match 헤더와 요청 본문의 version은
                    반드시 동일해야 합니다.
                    삭제에 성공하면 응답 본문 없이
                    204 No Content를 반환합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.VERSION_CONFLICT,
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
    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void>
    deleteProperty(
            @Parameter(
                    description = "삭제할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(
                    description = "마지막으로 조회한 매물 version",
                    required = true,
                    example = "\"2\""
            )
            @RequestHeader(
                    name = "If-Match",
                    required = false
            )
            String ifMatch,

            @Valid
            @RequestBody
            PropertyDeleteRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        Long ifMatchVersion = parseIfMatch(ifMatch);

        validateVersionAgreement(
                ifMatchVersion,
                request.version()
        );

        propertyDeleteService.delete(
                propertyId,
                request,
                actorContext
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "삭제된 매물 복구",
            description = """
                    CS 관리자 또는 최고 관리자가
                    소프트 삭제된 매물을 복구합니다.
                    복구하기 전에 매물이 참조하는 Region이
                    현재 활성 상태인지 논리적으로 검증합니다.
                    If-Match 헤더와 요청 본문의 version은
                    반드시 동일해야 합니다.
                    """
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.FORBIDDEN,
            CustomResponseCode.INVALID_REQUEST,
            CustomResponseCode.VERSION_CONFLICT,
            CustomResponseCode.PROPERTY_NOT_FOUND,
            CustomResponseCode.RESTORE_REFERENCE_INVALID,
            CustomResponseCode.DB_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PreAuthorize(
            "hasAnyRole("
                    + "'CS_ADMIN', "
                    + "'SUPER_ADMIN'"
                    + ")"
    )
    @PostMapping("/{propertyId}/restore")
    public ResponseEntity<
            GlobalResponseDTO<PropertyRestoreResponse>
            >
    restoreProperty(
            @Parameter(
                    description = "복구할 매물 ID",
                    required = true,
                    example = "884685586571263701"
            )
            @PathVariable
            Long propertyId,

            @Parameter(
                    description = "삭제된 매물의 현재 version",
                    required = true,
                    example = "\"3\""
            )
            @RequestHeader(
                    name = "If-Match",
                    required = false
            )
            String ifMatch,

            @Valid
            @RequestBody
            PropertyRestoreRequest request,

            @Parameter(hidden = true)
            ActorContext actorContext
    ) {
        Long ifMatchVersion = parseIfMatch(ifMatch);

        validateVersionAgreement(
                ifMatchVersion,
                request.version()
        );

        PropertyRestoreResponse response =
                propertyRestoreService.restore(
                        propertyId,
                        request,
                        actorContext
                );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    private Long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "If-Match 헤더는 필수입니다."
            );
        }

        String normalizedValue = ifMatch.trim();

        boolean startsWithQuote =
                normalizedValue.startsWith("\"");

        boolean endsWithQuote =
                normalizedValue.endsWith("\"");

        if (startsWithQuote != endsWithQuote) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "If-Match 헤더의 따옴표 형식이 올바르지 않습니다."
            );
        }

        if (startsWithQuote) {
            normalizedValue =
                    normalizedValue.substring(
                            1,
                            normalizedValue.length() - 1
                    );
        }

        try {
            Long version = Long.valueOf(normalizedValue);

            if (version < 0) {
                throw new BusinessException(
                        CustomResponseCode.INVALID_REQUEST,
                        "If-Match version은 0 이상이어야 합니다."
                );
            }

            return version;
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "If-Match 헤더에는 숫자 version을 입력해야 합니다."
            );
        }
    }

    private void validateVersionAgreement(
            Long ifMatchVersion,
            Long requestVersion
    ) {
        if (!Objects.equals(
                ifMatchVersion,
                requestVersion
        )) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "If-Match version과 요청 본문의 version이 일치하지 않습니다."
            );
        }
    }
}
