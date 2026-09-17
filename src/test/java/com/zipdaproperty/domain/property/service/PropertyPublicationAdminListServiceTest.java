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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyPublicationAdminListServiceTest {
    private final PropertyPublicationAdminListQueryRepository repository = mock(PropertyPublicationAdminListQueryRepository.class);
    private final PropertyPublicationAdminListService service = new PropertyPublicationAdminListService(
        repository, new PropertyAdminReviewAccess());
    private final ActorContext admin = ActorContext.member(1L, ActorRole.CS_ADMIN, "test");

    @Test
    void defaultFilterAndPageReturnPageMetadata() {
        Instant time = Instant.ofEpochSecond(42, 7);
        var first = new PropertyPublicationAdminListItemResponse(10L, 2L, "서울 강남구 역삼동",
            null, null, null, null, 1001L, PublicationStatus.IN_REVIEW,
            VerificationStatus.OWNER_VERIFIED, PublisherType.DIRECT_OWNER, PropertyType.APARTMENT, time);
        when(repository.find(eq(PublicationStatus.IN_REVIEW), eq(null), eq(null), eq(null),
            eq(null), eq(null), eq(null), any())).thenReturn(
            new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2));
        var result = service.find(new PropertyPublicationAdminListRequest(
            null, null, null, null, null, null, null, null, 1
        ), admin);
        assertThat(result.content()).containsExactly(first);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void addressAndRegisteredPeriodAreForwarded() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 2);
        Instant expectedFrom = from.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        Instant expectedToExclusive = to.plusDays(1)
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant();
        when(repository.find(eq(PublicationStatus.IN_REVIEW), eq(null), eq(null), eq(null),
            eq("강남구"), eq(expectedFrom), eq(expectedToExclusive),
            any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = service.find(new PropertyPublicationAdminListRequest(
            null, null, null, null, " 강남구 ", from, to, null, 20
        ), admin);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
