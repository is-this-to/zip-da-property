package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public record PropertyReportAdminListCursor(Instant createdAt, Long reportId) {

    private static final String DELIMITER = ":";

    public PropertyReportAdminListCursor {
        if ((createdAt == null) != (reportId == null)) {
            throw new IllegalArgumentException("cursor 구성값은 모두 존재하거나 모두 없어야 합니다.");
        }
        if (reportId != null && reportId <= 0) {
            throw new IllegalArgumentException("cursor reportId는 0보다 커야 합니다.");
        }
    }

    public static PropertyReportAdminListCursor firstPage() {
        return new PropertyReportAdminListCursor(null, null);
    }

    public static PropertyReportAdminListCursor from(Instant createdAt, Long reportId) {
        return new PropertyReportAdminListCursor(
                Objects.requireNonNull(createdAt, "cursor createdAt은 필수입니다."),
                Objects.requireNonNull(reportId, "cursor reportId는 필수입니다.")
        );
    }

    public static PropertyReportAdminListCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return firstPage();
        }

        try {
            String decodedValue = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] cursorParts = decodedValue.split(DELIMITER, -1);
            if (cursorParts.length != 3) {
                throw new IllegalArgumentException("cursor 구성값 개수가 올바르지 않습니다.");
            }

            Instant createdAt = Instant.ofEpochSecond(
                    Long.parseLong(cursorParts[0]),
                    Integer.parseInt(cursorParts[1])
            );
            long reportId = Long.parseLong(cursorParts[2]);
            return new PropertyReportAdminListCursor(createdAt, reportId);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "cursor 형식이 올바르지 않습니다."
            );
        }
    }

    public String encode() {
        if (createdAt == null || reportId == null) {
            return null;
        }

        String rawCursor = createdAt.getEpochSecond()
                + DELIMITER + createdAt.getNano()
                + DELIMITER + reportId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }
}
