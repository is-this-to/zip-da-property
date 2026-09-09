package com.zipdaproperty.domain.location.client;

import com.zipdaproperty.domain.location.client.response.KakaoAddressSearchApiResponse;
import com.zipdaproperty.domain.location.exception.KakaoLocalApiException;
import com.zipdaproperty.global.config.external.KakaoLocalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoLocalClient {

    private final RestClient restClient;
    private final KakaoLocalProperties properties;

    public KakaoLocalClient(
            @Qualifier("kakaoLocalRestClient")
            RestClient restClient,
            KakaoLocalProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KakaoAddressSearchApiResponse searchAddress(
            String query
    ) {
        try {
            KakaoAddressSearchApiResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", query)
                            .queryParam("size", properties.pageSize())
                            .build()
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, responseError) -> {
                                log.warn(
                                        "Kakao Local API 호출 실패. status={}",
                                        responseError.getStatusCode()
                                );

                                throw new KakaoLocalApiException(
                                        "카카오 주소 검색 API 호출에 실패했습니다."
                                );
                            }
                    )
                    .body(KakaoAddressSearchApiResponse.class);

            if (response == null) {
                throw new KakaoLocalApiException(
                        "카카오 주소 검색 API의 응답이 비어 있습니다."
                );
            }

            return response;
        } catch (KakaoLocalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn(
                    "Kakao Local API 통신 중 오류가 발생했습니다. exception={}",
                    exception.getClass().getSimpleName()
            );

            throw new KakaoLocalApiException(
                    "카카오 주소 검색 API와 통신할 수 없습니다."
            );
        }
    }
}
