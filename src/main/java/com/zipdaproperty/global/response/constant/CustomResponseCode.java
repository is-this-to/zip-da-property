package com.zipdaproperty.global.response.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomResponseCode {

    SUCCESS(HttpStatus.OK, "00"),

    // 인증·권한 관련
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "E03"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E04"),

    // 공통 리소스 관련
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND, "E10"),
    DUPLICATED_RESOURCE(HttpStatus.CONFLICT, "E11"),
    RESOURCE_AUTHOR_MISMATCH_ERROR(HttpStatus.FORBIDDEN, "E12"),

    // 입력값 검증 관련
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E21"),

    // HTTP 요청 방식 관련
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E22"),

    // Property 비즈니스 규칙 관련
    INVALID_PRICE_COMBINATION(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "P01"
    ),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "P02"),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "P03"),
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "P10"),
    PROPERTY_CREATE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "P11"),
    PROPERTY_OWNERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "P12"),

    // 찜 관련
    FAVORITE_TARGET_UNAVAILABLE(HttpStatus.NOT_FOUND, "P13"),

    // 옵션 관련
    OPTION_CODE_NOT_FOUND(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "P14"
    ),
    OPTION_NOT_ALLOWED_FOR_PROPERTY_TYPE(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "P15"
    ),
    OPTION_VALUE_INVALID(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "P16"
    ),
    OPTION_VALUE_REQUIRED(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "P17"
    ),

    // Property 멱등 요청 관련
    IDEMPOTENCY_KEY_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "P20"
    ),
    IDEMPOTENCY_CONFLICT(
            HttpStatus.CONFLICT,
            "P21"
    ),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "P22"
    ),

    // 파일 처리 관련
    FILE_MANAGED_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "E40"
    ),

    // DB 관련
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E80"),
    DB_DUPLICATED_KEY_ERROR(HttpStatus.CONFLICT, "E81"),

    // 시스템 관련
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99");

    private final HttpStatus httpStatus;
    private final String code;

    CustomResponseCode(
            HttpStatus httpStatus,
            String code
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}