package com.zipdaproperty.global.response;

public record FieldErrorDTO(
        String field,
        String message
) {
}