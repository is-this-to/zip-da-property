package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyAdminReviewAccessTest {
    private final PropertyAdminReviewAccess access = new PropertyAdminReviewAccess();

    @Test
    void onlyCsAndSuperAdminCanRead() {
        access.requireAdmin(ActorContext.member(1L, ActorRole.CS_ADMIN, "test"));
        access.requireAdmin(ActorContext.member(1L, ActorRole.SUPER_ADMIN, "test"));
        assertThatThrownBy(() -> access.requireAdmin(ActorContext.member(1L, ActorRole.USER, "test")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> access.requireAdmin(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void detailRequiresAuditReason() {
        assertThatThrownBy(() -> access.requireReason("  ")).isInstanceOf(BusinessException.class);
        assertThat(access.requireReason("  검수  ")).isEqualTo("검수");
        assertThatThrownBy(() -> access.requireReason("x".repeat(201))).isInstanceOf(BusinessException.class);
    }

    @Test
    void cursorRoundTripsNanosecondsAndRejectsInvalidValues() {
        PropertyAdminReviewCursor cursor = new PropertyAdminReviewCursor(Instant.ofEpochSecond(42, 123), 99L);
        assertThat(PropertyAdminReviewCursor.decode(cursor.encode())).isEqualTo(cursor);
        assertThatThrownBy(() -> PropertyAdminReviewCursor.decode("wrong"))
                .isInstanceOf(BusinessException.class);
    }
}
