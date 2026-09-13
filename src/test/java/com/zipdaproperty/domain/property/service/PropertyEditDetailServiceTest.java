package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.repository.PropertyRepository;
import com.zipdaproperty.domain.property.response.PropertyEditDetailResponse;
import com.zipdaproperty.domain.property.response.PropertyEditImageResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

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

    private final PropertyImageRepository propertyImageRepository =
            mock(PropertyImageRepository.class);

    private final PropertyFileRepository propertyFileRepository =
            mock(PropertyFileRepository.class);

    private final MinioPresignedGetUrlGenerator getUrlGenerator =
            mock(MinioPresignedGetUrlGenerator.class);

    private final PropertyEditDetailService
            propertyEditDetailService =
            new PropertyEditDetailService(
                    propertyRepository,
                    propertyImageRepository,
                    propertyFileRepository,
                    getUrlGenerator
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

        assertThat(response.images()).isEmpty();
        verifyNoInteractions(
                propertyFileRepository,
                getUrlGenerator
        );

        verify(propertyRepository)
                .findByPropertyIdAndDeletedAtIsNull(
                        PROPERTY_ID
                );
    }

    @Test
    void getEditDetail_activeImagesAndFiles_returnsOrderedImageUrls() {
        Property property = prepareProperty();
        PropertyImage representative = PropertyImage.create(
                PROPERTY_ID,
                101L,
                0,
                true,
                null,
                authorContext
        );
        PropertyImage second = PropertyImage.create(
                PROPERTY_ID,
                102L,
                1,
                false,
                null,
                authorContext
        );
        PropertyFile representativeFile = propertyFile(
                101L,
                "opaque-image-reference-101"
        );
        PropertyFile secondFile = propertyFile(
                102L,
                "opaque-image-reference-102"
        );
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(
                PROPERTY_ID
        )).thenReturn(Optional.of(property));
        when(propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        PROPERTY_ID
                )).thenReturn(List.of(representative, second));
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(
                        List.of(101L, 102L)
                )).thenReturn(List.of(secondFile, representativeFile));
        when(getUrlGenerator.generate("opaque-image-reference-101"))
                .thenReturn("https://example.test/image-101");
        when(getUrlGenerator.generate("opaque-image-reference-102"))
                .thenReturn("https://example.test/image-102");

        PropertyEditDetailResponse response =
                propertyEditDetailService.getEditDetail(
                        PROPERTY_ID,
                        authorContext
                );

        assertThat(response.images()).hasSize(2);
        assertThat(response.images().get(0).fileId()).isEqualTo(101L);
        assertThat(response.images().get(0).imageUrl())
                .isEqualTo("https://example.test/image-101");
        assertThat(response.images().get(0).sortOrder()).isZero();
        assertThat(response.images().get(0).representative()).isTrue();
        assertThat(response.images().get(1).fileId()).isEqualTo(102L);
        assertThat(response.images().get(1).imageUrl())
                .isEqualTo("https://example.test/image-102");
        assertThat(response.images().get(1).sortOrder()).isEqualTo(1);
        assertThat(response.images().get(1).representative()).isFalse();
        assertThat(PropertyEditImageResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly(
                        "fileId",
                        "imageUrl",
                        "sortOrder",
                        "representative"
                );
    }

    @Test
    void getEditDetail_deletedFileLink_omitsImageWithoutIssuingUrl() {
        Property property = prepareProperty();
        PropertyImage image = PropertyImage.create(
                PROPERTY_ID,
                101L,
                0,
                true,
                null,
                authorContext
        );
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(
                PROPERTY_ID
        )).thenReturn(Optional.of(property));
        when(propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
                        PROPERTY_ID
                )).thenReturn(List.of(image));
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(
                        List.of(101L)
                )).thenReturn(List.of());

        PropertyEditDetailResponse response =
                propertyEditDetailService.getEditDetail(
                        PROPERTY_ID,
                        authorContext
                );

        assertThat(response.images()).isEmpty();
        verifyNoInteractions(getUrlGenerator);
    }

    @Test
    void getEditDetail_verifiedFile_omitsImageWithoutIssuingUrl() {
        assertUnavailableFileIsOmitted(UploadStatus.VERIFIED,
                FilePurpose.PROPERTY_IMAGE, null);
    }

    @Test
    void getEditDetail_nonPropertyImageFile_omitsImageWithoutIssuingUrl() {
        assertUnavailableFileIsOmitted(UploadStatus.LINKED,
                FilePurpose.VERIFICATION, null);
    }

    @Test
    void getEditDetail_objectDeletedFile_omitsImageWithoutIssuingUrl() {
        assertUnavailableFileIsOmitted(UploadStatus.LINKED,
                FilePurpose.PROPERTY_IMAGE, Instant.now());
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

    private PropertyFile propertyFile(
            Long fileId,
            String objectKey
    ) {
        PropertyFile propertyFile = mock(PropertyFile.class);
        when(propertyFile.getPropertyFileId()).thenReturn(fileId);
        when(propertyFile.getObjectKey()).thenReturn(objectKey);
        when(propertyFile.getUploadStatus()).thenReturn(UploadStatus.LINKED);
        when(propertyFile.getFilePurpose()).thenReturn(FilePurpose.PROPERTY_IMAGE);
        when(propertyFile.getObjectDeletedAt()).thenReturn(null);
        return propertyFile;
    }

    private void assertUnavailableFileIsOmitted(
            UploadStatus uploadStatus,
            FilePurpose filePurpose,
            Instant objectDeletedAt
    ) {
        Property property = prepareProperty();
        PropertyImage image = PropertyImage.create(
                PROPERTY_ID, 101L, 0, true, null, authorContext
        );
        PropertyFile file = propertyFile(101L, "opaque-image-reference-101");
        when(file.getUploadStatus()).thenReturn(uploadStatus);
        when(file.getFilePurpose()).thenReturn(filePurpose);
        when(file.getObjectDeletedAt()).thenReturn(objectDeletedAt);
        when(propertyRepository.findByPropertyIdAndDeletedAtIsNull(PROPERTY_ID))
                .thenReturn(Optional.of(property));
        when(propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(PROPERTY_ID))
                .thenReturn(List.of(image));
        when(propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(101L)))
                .thenReturn(List.of(file));

        PropertyEditDetailResponse response = propertyEditDetailService
                .getEditDetail(PROPERTY_ID, authorContext);

        assertThat(response.images()).isEmpty();
        verifyNoInteractions(getUrlGenerator);
    }
}
