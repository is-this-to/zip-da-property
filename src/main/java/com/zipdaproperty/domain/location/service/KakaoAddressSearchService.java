package com.zipdaproperty.domain.location.service;

import com.zipdaproperty.domain.location.client.KakaoLocalClient;
import com.zipdaproperty.domain.location.client.response.KakaoAddressSearchApiResponse;
import com.zipdaproperty.domain.location.request.KakaoAddressSearchRequest;
import com.zipdaproperty.domain.location.response.KakaoAddressSearchResponse;
import com.zipdaproperty.global.error.custom.business.KakaoLocalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoAddressSearchService {

    private final KakaoLocalClient kakaoLocalClient;

    public KakaoAddressSearchResponse search(
            KakaoAddressSearchRequest request
    ) {
        KakaoAddressSearchApiResponse apiResponse =
                kakaoLocalClient.searchAddress(request.query());

        List<KakaoAddressSearchResponse.Item> items =
                new ArrayList<>();

        for (KakaoAddressSearchApiResponse.Document document
                : apiResponse.documents()) {
            KakaoAddressSearchResponse.Item item =
                    convertToItem(document);

            if (item != null) {
                items.add(item);
            }
        }

        return new KakaoAddressSearchResponse(items);
    }

    private KakaoAddressSearchResponse.Item convertToItem(
            KakaoAddressSearchApiResponse.Document document
    ) {
        if (document == null || document.address() == null) {
            return null;
        }

        String legalDongCode = normalizeNullable(
                document.address().legalDongCode()
        );
        String longitudeValue = normalizeNullable(document.x());
        String latitudeValue = normalizeNullable(document.y());

        if (legalDongCode == null
                || longitudeValue == null
                || latitudeValue == null) {
            return null;
        }

        try {
            BigDecimal longitude =
                    new BigDecimal(longitudeValue);
            BigDecimal latitude =
                    new BigDecimal(latitudeValue);

            String roadAddress = document.roadAddress() == null
                    ? null
                    : normalizeNullable(
                            document.roadAddress().addressName()
                    );

            String buildingName = document.roadAddress() == null
                    ? null
                    : normalizeNullable(
                            document.roadAddress().buildingName()
                    );

            String jibunAddress = normalizeNullable(
                    document.address().addressName()
            );

            return new KakaoAddressSearchResponse.Item(
                    roadAddress,
                    jibunAddress,
                    legalDongCode,
                    buildingName,
                    longitude,
                    latitude
            );
        } catch (NumberFormatException exception) {
            throw new KakaoLocalApiException(
                    "카카오 주소 검색 응답의 좌표 형식이 올바르지 않습니다."
            );
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
