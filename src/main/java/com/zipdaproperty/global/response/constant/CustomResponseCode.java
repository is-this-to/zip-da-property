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
    // 요청 URL은 존재하지만 지원하지 않는 HTTP Method로 호출한 경우
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E22"),

    // Property 비즈니스 규칙 관련 (Property 관련은 "P00"으로 작성)
    INVALID_PRICE_COMBINATION(HttpStatus.UNPROCESSABLE_CONTENT, "P01"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "P02"),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "P03"),
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "P10"),
    PROPERTY_CREATE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "P11"),
    PROPERTY_OWNERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "P12"),


    // 찜 대상 매물이 존재하지 않거나 공개·거래 가능한 상태가 아닌 경우
    FAVORITE_TARGET_UNAVAILABLE(HttpStatus.NOT_FOUND, "P13"),

    // 옵션 관련
    OPTION_CODE_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "P14"),
    OPTION_NOT_ALLOWED_FOR_PROPERTY_TYPE(HttpStatus.UNPROCESSABLE_CONTENT, "P15"),
    OPTION_VALUE_TYPE_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "P16"),
    OPTION_VALUE_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "P17"),


    // 파일 처리 관련
    FILE_MANAGED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E40"),

    // DB 관련
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E80"),
    DB_DUPLICATED_KEY_ERROR(HttpStatus.CONFLICT, "E81"),

    // 시스템 관련
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E99");

    private final HttpStatus httpStatus;
    private final String code;

    CustomResponseCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}