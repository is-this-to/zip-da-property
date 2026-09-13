package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.model.PropertyListCursorProperties;
import com.zipdaproperty.domain.property.model.PropertyPublicListCursor;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class PropertyPublicListCursorCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String NULL_VALUE = "~";

    private final byte[] secret;
    private final String secretVersion;

    public PropertyPublicListCursorCodec(PropertyListCursorProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("PROPERTY_LIST_CURSOR_SECRET이 필요합니다.");
        }
        if (properties.secretVersion() == null || properties.secretVersion().isBlank()) {
            throw new IllegalStateException("PROPERTY_LIST_CURSOR_SECRET_VERSION이 필요합니다.");
        }
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.secretVersion = properties.secretVersion();
    }

    public String encode(PropertyPublicListCursor cursor) {
        Instant createdAt = cursor.createdAt();
        String payload = String.join("|",
                secretVersion,
                cursor.sort().name(),
                cursor.contextHash(),
                createdAt == null ? NULL_VALUE : Long.toString(createdAt.getEpochSecond()),
                createdAt == null ? NULL_VALUE : Integer.toString(createdAt.getNano()),
                nullable(cursor.representativePrice()),
                nullable(cursor.deposit()),
                nullableDecimal(cursor.exclusiveArea()),
                cursor.propertyId().toString()
        );

        return encodeBase64(payload.getBytes(StandardCharsets.UTF_8))
                + "."
                + encodeBase64(sign(payload));
    }

    public PropertyPublicListCursor decode(
            String token,
            PropertyMapSort expectedSort,
            String expectedContextHash
    ) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String[] tokenParts = token.split("\\.", -1);
            if (tokenParts.length != 2) {
                throw new IllegalArgumentException();
            }

            String payload = new String(
                    Base64.getUrlDecoder().decode(tokenParts[0]),
                    StandardCharsets.UTF_8
            );
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(tokenParts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) {
                throw new IllegalArgumentException();
            }

            String[] values = payload.split("\\|", -1);
            if (values.length != 9 || !secretVersion.equals(values[0])) {
                throw new IllegalArgumentException();
            }

            PropertyMapSort sort = PropertyMapSort.valueOf(values[1]);
            if (sort != expectedSort || !values[2].equals(expectedContextHash)) {
                throw new IllegalArgumentException();
            }

            return new PropertyPublicListCursor(
                    sort,
                    values[2],
                    parseInstant(values[3], values[4]),
                    parseLong(values[5]),
                    parseLong(values[6]),
                    parseDecimal(values[7]),
                    Long.valueOf(values[8])
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "cursor가 유효하지 않거나 현재 검색 조건과 일치하지 않습니다."
            );
        }
    }

    private Instant parseInstant(String seconds, String nanos) {
        if (NULL_VALUE.equals(seconds) && NULL_VALUE.equals(nanos)) {
            return null;
        }
        if (NULL_VALUE.equals(seconds) || NULL_VALUE.equals(nanos)) {
            throw new IllegalArgumentException();
        }
        return Instant.ofEpochSecond(Long.parseLong(seconds), Integer.parseInt(nanos));
    }

    private Long parseLong(String value) {
        return NULL_VALUE.equals(value) ? null : Long.valueOf(value);
    }

    private BigDecimal parseDecimal(String value) {
        return NULL_VALUE.equals(value) ? null : new BigDecimal(value);
    }

    private String nullable(Long value) {
        return value == null ? NULL_VALUE : value.toString();
    }

    private String nullableDecimal(BigDecimal value) {
        return value == null ? NULL_VALUE : value.stripTrailingZeros().toPlainString();
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("cursor 서명을 생성할 수 없습니다.", exception);
        }
    }

    private String encodeBase64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
