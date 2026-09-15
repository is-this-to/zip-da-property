package com.zipdaproperty.global.config.external;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "external-api.kakao-local")
public record KakaoLocalProperties(
    @NotBlank
    String baseUrl,

    @NotBlank
    String restApiKey,

    @NotNull
    Duration connectTimeout,

    @NotNull
    Duration readTimeout,

    @Min(1)
    @Max(30)
    int pageSize
) {
    public KakaoLocalProperties{
        if(baseUrl != null){
            baseUrl = removeTrailingSlash(baseUrl.trim());
        }

        if(restApiKey != null){
            restApiKey = restApiKey.trim();
        }
    }

    private static String removeTrailingSlash(String value){
        while(value.endsWith("/")){
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }
}
