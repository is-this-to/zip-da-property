package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyEditDetailServiceTest {

    private static final Long PROPERTY_ID =
            884685586571263701L;

    private static final Long REGION_ID = 53390L;

    private static final Long AUTHOR_MEMBER_ID = 1001L;

    private static final Long OTHER_MEMBER_ID = 2002L;

    private static final Long ADMIN_MEMBER_ID = 3003L;

    private static final Long CURRENT_VERSION = 4L;

    private final PropertyRepository propertyRepository =
            mock(PropertyRepository.class);

    private final PropertyEditDetailService
            propertyEditDetailService =
            new PropertyEditDetailService(
                    propertyRepository
            );

    private final ActorContext authorContext =
            ActorContext.member(
                    AUTHOR_MEMBER_ID,
                    ActorRole.USER,
                    "property-edit-detail-author-test"
            );

    @Test
    void getEditDetail_author_returnsCurrentPropertyData() {
        Property property = prepareProperty();

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(property));

        PropertyEditDetailResponse response =
                propertyEditDetailService.getEditDetail(
                        PROPERTY_ID,
                        authorContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(CURRENT_VERSION);

        assertThat(response.regionId())
                .isEqualTo(REGION_ID);

        assertThat(response.publisherType())
                .isEqualTo(PublisherType.DIRECT_OWNER);

        assertThat(response.propertyType())
                .isEqualTo(PropertyType.APARTMENT);

        assertThat(response.transactionType())
                .isEqualTo(TransactionType.SALE);

        assertThat(response.salePrice())
                .isEqualTo(500_000_000L);

        assertThat(response.exclusiveArea())
                .isEqualByComparingTo("59.99");

        assertThat(response.title())
                .isEqualTo("수정용 상세 조회 테스트 매물");

        assertThat(response.publicationStatus())
                .isEqualTo(PublicationStatus.IN_REVIEW);

        assertThat(response.transactionStatus())
                .isEqualTo(TransactionStatus.RESERVED);

        assertThat(response.verificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);

        verify(propertyRepository)
                .findByPropertyIdAndDeletedAtIsNull(
                        PROPERTY_ID
                );
    }

    @Test
    void getEditDetail_allowedAdmin_returnsPropertyData() {
        Property property = prepareProperty();

        ActorContext adminContext =
                ActorContext.member(
                        ADMIN_MEMBER_ID,
                        ActorRole.CS_ADMIN,
                        "property-edit-detail-admin-test"
                );

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(property));

        PropertyEditDetailResponse response =
                propertyEditDetailService.getEditDetail(
                        PROPERTY_ID,
                        adminContext
                );

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.version())
                .isEqualTo(CURRENT_VERSION);
    }

    @Test
    void getEditDetail_otherMember_throwsOwnershipRequired() {
        Property property = prepareProperty();

        ActorContext otherMemberContext =
                ActorContext.member(
                        OTHER_MEMBER_ID,
                        ActorRole.USER,
                        "property-edit-detail-other-member-test"
                );

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(property));

        assertThatThrownBy(
                () -> propertyEditDetailService
                        .getEditDetail(
                                PROPERTY_ID,
                                otherMemberContext
                        )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(
                                        exception
                                                .getCustomResponseCode()
                                ).isEqualTo(
                                        CustomResponseCode
                                                .PROPERTY_OWNERSHIP_REQUIRED
                                )
                );
    }

    @Test
    void getEditDetail_deletedOrMissingProperty_throwsNotFound() {
        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> propertyEditDetailService
                        .getEditDetail(
                                PROPERTY_ID,
                                authorContext
                        )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(
                                        exception
                                                .getCustomResponseCode()
                                ).isEqualTo(
                                        CustomResponseCode
                                                .PROPERTY_NOT_FOUND
                                )
                );
    }

    @Test
    void getEditDetail_invalidPropertyId_throwsNotFoundWithoutQuery() {
        assertThatThrownBy(
                () -> propertyEditDetailService
                        .getEditDetail(
                                0L,
                                authorContext
                        )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(
                                        exception
                                                .getCustomResponseCode()
                                ).isEqualTo(
                                        CustomResponseCode
                                                .PROPERTY_NOT_FOUND
                                )
                );

        verifyNoInteractions(propertyRepository);
    }

    @Test
    void getEditDetail_systemActor_throwsOwnershipRequired() {
        Property property = prepareProperty();

        ActorContext systemContext =
                ActorContext.system(
                        "property-edit-detail-system-test"
                );

        when(
                propertyRepository
                        .findByPropertyIdAndDeletedAtIsNull(
                                PROPERTY_ID
                        )
        ).thenReturn(Optional.of(property));

        assertThatThrownBy(
                () -> propertyEditDetailService
                        .getEditDetail(
                                PROPERTY_ID,
                                systemContext
                        )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(
                                        exception
                                                .getCustomResponseCode()
                                ).isEqualTo(
                                        CustomResponseCode
                                                .PROPERTY_OWNERSHIP_REQUIRED
                                )
                );
    }

    private Property prepareProperty() {
        Property property = mock(Property.class);

        when(property.getPropertyId())
                .thenReturn(PROPERTY_ID);

        when(property.getVersion())
                .thenReturn(CURRENT_VERSION);

        when(property.getRegionId())
                .thenReturn(REGION_ID);

        when(property.getAuthorMemberId())
                .thenReturn(AUTHOR_MEMBER_ID);

        when(property.getPublisherType())
                .thenReturn(PublisherType.DIRECT_OWNER);

        when(property.getPropertyType())
                .thenReturn(PropertyType.APARTMENT);

        when(property.getTransactionType())
                .thenReturn(TransactionType.SALE);

        when(property.getSalePrice())
                .thenReturn(500_000_000L);

        when(property.getExclusiveArea())
                .thenReturn(new BigDecimal("59.99"));

        when(property.getTitle())
                .thenReturn("수정용 상세 조회 테스트 매물");

        when(property.getDescription())
                .thenReturn("수정 화면에 기존 값을 표시하기 위한 테스트입니다.");

        when(property.getPublicationStatus())
                .thenReturn(PublicationStatus.IN_REVIEW);

        when(property.getTransactionStatus())
                .thenReturn(TransactionStatus.RESERVED);

        when(property.getVerificationStatus())
                .thenReturn(VerificationStatus.UNVERIFIED);

        return property;
    }
}