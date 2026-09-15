package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record PropertyAdminReviewCursor(Instant time, Long id) {
    public static PropertyAdminReviewCursor decode(String cursor) {
        if (cursor == null) return new PropertyAdminReviewCursor(null, null);
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":", -1);
            if (parts.length != 3) throw new IllegalArgumentException();
            Instant time = Instant.ofEpochSecond(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
            Long id = Long.valueOf(parts[2]);
            if (id <= 0) throw new IllegalArgumentException();
            return new PropertyAdminReviewCursor(time, id);
        } catch (RuntimeException exception) {
            throw new BusinessException(CustomResponseCode.INVALID_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }
    }

    public String encode() {
        if (time == null || id == null) return null;
        String raw = time.getEpochSecond() + ":" + time.getNano() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
