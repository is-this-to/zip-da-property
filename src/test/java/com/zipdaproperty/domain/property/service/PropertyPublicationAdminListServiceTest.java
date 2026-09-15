package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.repository.PropertyPublicationAdminListQueryRepository;
import com.zipdaproperty.domain.property.request.PropertyPublicationAdminListRequest;
import com.zipdaproperty.domain.property.response.PropertyPublicationAdminListItemResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyPublicationAdminListServiceTest {
    private final PropertyPublicationAdminListQueryRepository repository = mock(PropertyPublicationAdminListQueryRepository.class);
    private final PropertyPublicationAdminListService service = new PropertyPublicationAdminListService(
            repository, new PropertyAdminReviewAccess());
    private final ActorContext admin = ActorContext.member(1L, ActorRole.CS_ADMIN, "test");

    @Test
    void defaultFilterAndCursorUseCreatedAtAndPropertyId() {
        Instant time = Instant.ofEpochSecond(42, 7);
        var first = new PropertyPublicationAdminListItemResponse(10L, 2L, PublicationStatus.IN_REVIEW,
                VerificationStatus.OWNER_VERIFIED, PublisherType.DIRECT_OWNER, PropertyType.APARTMENT, time);
        var second = new PropertyPublicationAdminListItemResponse(11L, 2L, PublicationStatus.IN_REVIEW,
                VerificationStatus.OWNER_VERIFIED, PublisherType.DIRECT_OWNER, PropertyType.APARTMENT, time);
        when(repository.find(eq(PublicationStatus.IN_REVIEW), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(2))).thenReturn(List.of(first, second));
        var result = service.find(new PropertyPublicationAdminListRequest(null, null, null, null, null, 1), admin);
        assertThat(result.items()).containsExactly(first);
        assertThat(PropertyAdminReviewCursor.decode(result.nextCursor()))
                .isEqualTo(new PropertyAdminReviewCursor(time, 10L));
        assertThat(result.hasNext()).isTrue();
    }
}
