package com.zipdaproperty.global.config.external;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "external-api.member")
public record MemberServiceProperties(
        @NotBlank
        String baseUrl,

        @NotNull
        Duration connectTimeout,

        @NotNull
        Duration readTimeout
) {
    public MemberServiceProperties {
        if (baseUrl != null) {
            baseUrl = removeTrailingSlash(baseUrl.trim());
        }
    }

    private static String removeTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
