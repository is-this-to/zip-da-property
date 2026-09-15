package com.zipdaproperty.domain.property.verification.service;

import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.service.PropertyAdminReviewAccess;
import com.zipdaproperty.domain.property.service.PropertyAdminReviewCursor;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.repository.PropertyVerificationAdminListQueryRepository;
import com.zipdaproperty.domain.property.verification.request.PropertyVerificationAdminListRequest;
import com.zipdaproperty.domain.property.verification.response.PropertyVerificationAdminListItemResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyVerificationAdminListServiceTest {
    private final PropertyVerificationAdminListQueryRepository repository = mock(PropertyVerificationAdminListQueryRepository.class);
    private final PropertyVerificationAdminListService service = new PropertyVerificationAdminListService(
            repository, new PropertyAdminReviewAccess());

    @Test
    void defaultFilterAndCursorUseSubmittedAtAndVerificationId() {
        Instant time = Instant.ofEpochSecond(42, 7);
        var first = new PropertyVerificationAdminListItemResponse(10L, 20L, 2L, 1,
                PropertyVerificationType.OWNER, PropertyVerificationStatus.IN_REVIEW,
                VerificationStatus.IN_REVIEW, time);
        var second = new PropertyVerificationAdminListItemResponse(10L, 21L, 2L, 1,
                PropertyVerificationType.OWNER, PropertyVerificationStatus.IN_REVIEW,
                VerificationStatus.IN_REVIEW, time);
        when(repository.find(eq(PropertyVerificationStatus.IN_REVIEW), eq(null),
                eq(null), eq(null), eq(2))).thenReturn(List.of(first, second));
        var result = service.find(new PropertyVerificationAdminListRequest(null, null, null, 1),
                ActorContext.member(1L, ActorRole.SUPER_ADMIN, "test"));
        assertThat(result.items()).containsExactly(first);
        assertThat(PropertyAdminReviewCursor.decode(result.nextCursor()))
                .isEqualTo(new PropertyAdminReviewCursor(time, 20L));
        assertThat(result.hasNext()).isTrue();
    }
}
