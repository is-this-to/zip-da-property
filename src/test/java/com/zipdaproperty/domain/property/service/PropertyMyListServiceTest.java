package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.repository.PropertyMyListQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyMyListQueryRow;
import com.zipdaproperty.domain.property.request.PropertyMyListRequest;
import com.zipdaproperty.domain.property.response.PropertyMyListItemResponse;
import com.zipdaproperty.domain.property.response.PropertyMyListResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyMyListServiceTest {

    private static final Long MEMBER_ID =
            1001L;

    private static final Long ADMIN_MEMBER_ID =
            3003L;

    private static final Long FIRST_PROPERTY_ID =
            884685586571263701L;

    private static final Long SECOND_PROPERTY_ID =
            884685586571263702L;

    private static final Long REGION_ID =
            53390L;

    private static final Instant FIRST_UPDATED_AT =
            Instant.parse(
                    "2026-09-09T06:41:04.828Z"
            );

    private static final Instant SECOND_UPDATED_AT =
            Instant.parse(
                    "2026-09-09T05:41:04.828Z"
            );

    private final PropertyMyListQueryRepository
            propertyMyListQueryRepository =
            mock(PropertyMyListQueryRepository.class);

    private final PropertyMyListService
            propertyMyListService =
            new PropertyMyListService(
                    propertyMyListQueryRepository
            );

    private final ActorContext userContext =
            ActorContext.member(
                    MEMBER_ID,
                    ActorRole.USER,
                    "property-my-list-test-user"
            );

    @Test
    void findMyProperties_firstPage_returnsItemsWithoutNextCursor() {
        PropertyMyListRequest request =
                new PropertyMyListRequest(
                        null,
                        20
                );

        PropertyMyListQueryRow row =
                createRow(
                        FIRST_PROPERTY_ID,
                        FIRST_UPDATED_AT
                );

        when(
                propertyMyListQueryRepository
                        .findMyProperties(
                                MEMBER_ID,
                                null,
                                null,
                                21
                        )
        ).thenReturn(
                List.of(row)
        );

        PropertyMyListResponse response =
                propertyMyListService.findMyProperties(
                        request,
                        userContext
                );

        assertThat(response.items())
                .hasSize(1);

        PropertyMyListItemResponse item =
                response.items().get(0);

        assertThat(item.propertyId())
                .isEqualTo(FIRST_PROPERTY_ID);

        assertThat(item.version())
                .isEqualTo(4L);

        assertThat(item.regionId())
                .isEqualTo(REGION_ID);

        assertThat(item.title())
                .isEqualTo("내 매물 목록 테스트");

        assertThat(item.propertyType())
                .isEqualTo(PropertyType.APARTMENT);

        assertThat(item.transactionType())
                .isEqualTo(TransactionType.SALE);

        assertThat(item.publicationStatus())
                .isEqualTo(
                        PublicationStatus.IN_REVIEW
                );

        assertThat(item.transactionStatus())
                .isEqualTo(
                        TransactionStatus.AVAILABLE
                );

        assertThat(item.verificationStatus())
                .isEqualTo(
                        VerificationStatus.UNVERIFIED
                );

        assertThat(item.updatedAt())
                .isEqualTo(FIRST_UPDATED_AT);

        assertThat(response.hasNext())
                .isFalse();

        assertThat(response.nextCursor())
                .isNull();

        verify(propertyMyListQueryRepository)
                .findMyProperties(
                        MEMBER_ID,
                        null,
                        null,
                        21
                );
    }

    @Test
    void findMyProperties_moreRowsThanSize_returnsNextCursor() {
        PropertyMyListRequest request =
                new PropertyMyListRequest(
                        null,
                        1
                );

        PropertyMyListQueryRow firstRow =
                createRow(
                        FIRST_PROPERTY_ID,
                        FIRST_UPDATED_AT
                );

        PropertyMyListQueryRow secondRow =
                createRow(
                        SECOND_PROPERTY_ID,
                        SECOND_UPDATED_AT
                );

        when(
                propertyMyListQueryRepository
                        .findMyProperties(
                                MEMBER_ID,
                                null,
                                null,
                                2
                        )
        ).thenReturn(
                List.of(
                        firstRow,
                        secondRow
                )
        );

        PropertyMyListResponse response =
                propertyMyListService.findMyProperties(
                        request,
                        userContext
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(
                response.items()
                        .get(0)
                        .propertyId()
        ).isEqualTo(FIRST_PROPERTY_ID);

        assertThat(response.hasNext())
                .isTrue();

        assertThat(response.nextCursor())
                .isNotBlank();

        PropertyMyListCursor decodedCursor =
                PropertyMyListCursor.decode(
                        response.nextCursor()
                );

        assertThat(decodedCursor.updatedAt())
                .isEqualTo(FIRST_UPDATED_AT);

        assertThat(decodedCursor.propertyId())
                .isEqualTo(FIRST_PROPERTY_ID);
    }

    @Test
    void findMyProperties_encodedCursor_passesDecodedValuesToRepository() {
        PropertyMyListCursor cursor =
                PropertyMyListCursor.from(
                        FIRST_UPDATED_AT,
                        FIRST_PROPERTY_ID
                );

        PropertyMyListRequest request =
                new PropertyMyListRequest(
                        cursor.encode(),
                        20
                );

        when(
                propertyMyListQueryRepository
                        .findMyProperties(
                                MEMBER_ID,
                                FIRST_UPDATED_AT,
                                FIRST_PROPERTY_ID,
                                21
                        )
        ).thenReturn(List.of());

        PropertyMyListResponse response =
                propertyMyListService.findMyProperties(
                        request,
                        userContext
                );

        assertThat(response.items())
                .isEmpty();

        assertThat(response.hasNext())
                .isFalse();

        assertThat(response.nextCursor())
                .isNull();

        verify(propertyMyListQueryRepository)
                .findMyProperties(
                        MEMBER_ID,
                        FIRST_UPDATED_AT,
                        FIRST_PROPERTY_ID,
                        21
                );
    }

    @Test
    void findMyProperties_adminRole_throwsForbidden() {
        PropertyMyListRequest request =
                new PropertyMyListRequest(
                        null,
                        20
                );

        ActorContext adminContext =
                ActorContext.member(
                        ADMIN_MEMBER_ID,
                        ActorRole.CS_ADMIN,
                        "property-my-list-test-admin"
                );

        assertThatThrownBy(
                () -> propertyMyListService
                        .findMyProperties(
                                request,
                                adminContext
                        )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.FORBIDDEN
                        )
        );

        verifyNoInteractions(
                propertyMyListQueryRepository
        );
    }

    @Test
    void findMyProperties_invalidCursor_throwsInvalidRequest() {
        PropertyMyListRequest request =
                new PropertyMyListRequest(
                        "invalid-cursor",
                        20
                );

        assertThatThrownBy(
                () -> propertyMyListService
                        .findMyProperties(
                                request,
                                userContext
                        )
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                        assertThat(
                                exception.getCustomResponseCode()
                        ).isEqualTo(
                                CustomResponseCode.INVALID_REQUEST
                        )
        );

        verifyNoInteractions(
                propertyMyListQueryRepository
        );
    }

    private PropertyMyListQueryRow createRow(
            Long propertyId,
            Instant updatedAt
    ) {
        return new PropertyMyListQueryRow(
                propertyId,
                4L,
                REGION_ID,
                "내 매물 목록 테스트",
                PropertyType.APARTMENT,
                TransactionType.SALE,
                500_000_000L,
                null,
                null,
                PublisherType.DIRECT_OWNER,
                PublicationStatus.IN_REVIEW,
                TransactionStatus.AVAILABLE,
                VerificationStatus.UNVERIFIED,
                updatedAt
        );
    }
}