package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PropertyAddressNormalizer {

    public String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .trim()
                .replaceAll("\\s+", " ");

        return normalized.isEmpty() ? null : normalized;
    }

    public String createNormalizedAddressHash(
            String roadAddress,
            String jibunAddress
    ) {
        String normalizedRoadAddress =
                normalizeNullable(roadAddress);
        String normalizedJibunAddress =
                normalizeNullable(jibunAddress);

        String selectedAddress = normalizedRoadAddress != null
                ? normalizedRoadAddress
                : normalizedJibunAddress;

        if (selectedAddress == null) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "도로명 주소 또는 지번 주소 중 하나는 필수입니다."
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    selectedAddress.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "주소 해시를 생성할 수 없습니다.",
                    exception
            );
        }
    }
}
