package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailImageResponse;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropertyPublicImageServiceTest {

    private static final Long PROPERTY_ID = 884685586571263701L;

    private final PropertyImageRepository imageRepository =
            mock(PropertyImageRepository.class);
    private final PropertyFileRepository fileRepository =
            mock(PropertyFileRepository.class);
    private final MinioPresignedGetUrlGenerator getUrlGenerator =
            mock(MinioPresignedGetUrlGenerator.class);
    private final PropertyPublicImageService service =
            new PropertyPublicImageService(
                    imageRepository,
                    fileRepository,
                    getUrlGenerator
            );

    @Test
    void findImages_returnsOrderedLinkedImagesWithPresignedUrls() {
        PropertyImage first = PropertyImage.create(
                PROPERTY_ID, 101L, 0, true, null, actorContext()
        );
        PropertyImage second = PropertyImage.create(
                PROPERTY_ID, 102L, 1, false, null, actorContext()
        );
        PropertyFile firstFile = linkedImageFile(101L, "property/101.jpg");
        PropertyFile secondFile = linkedImageFile(102L, "property/102.jpg");
        when(imageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(PROPERTY_ID))
                .thenReturn(List.of(first, second));
        when(fileRepository.findAllByPropertyFileIdInAndDeletedAtIsNull(
                List.of(101L, 102L)
        )).thenReturn(List.of(secondFile, firstFile));
        when(getUrlGenerator.generate("property/101.jpg"))
                .thenReturn("https://example.test/101");
        when(getUrlGenerator.generate("property/102.jpg"))
                .thenReturn("https://example.test/102");

        List<PropertyPublicDetailImageResponse> images = service.findImages(PROPERTY_ID);

        assertThat(images).containsExactly(
                new PropertyPublicDetailImageResponse(101L, "https://example.test/101", 0, true),
                new PropertyPublicDetailImageResponse(102L, "https://example.test/102", 1, false)
        );
    }

    @Test
    void findImages_omitsUnavailableFileWithoutGeneratingUrl() {
        PropertyImage image = PropertyImage.create(
                PROPERTY_ID, 101L, 0, true, null, actorContext()
        );
        PropertyFile unavailableFile = linkedImageFile(101L, "property/101.jpg");
        when(unavailableFile.getUploadStatus()).thenReturn(UploadStatus.VERIFIED);
        when(imageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(PROPERTY_ID))
                .thenReturn(List.of(image));
        when(fileRepository.findAllByPropertyFileIdInAndDeletedAtIsNull(List.of(101L)))
                .thenReturn(List.of(unavailableFile));

        assertThat(service.findImages(PROPERTY_ID)).isEmpty();
        verifyNoInteractions(getUrlGenerator);
    }

    private PropertyFile linkedImageFile(Long fileId, String objectKey) {
        PropertyFile file = mock(PropertyFile.class);
        when(file.getPropertyFileId()).thenReturn(fileId);
        when(file.getObjectKey()).thenReturn(objectKey);
        when(file.getUploadStatus()).thenReturn(UploadStatus.LINKED);
        when(file.getFilePurpose()).thenReturn(FilePurpose.PROPERTY_IMAGE);
        when(file.getObjectDeletedAt()).thenReturn(null);
        return file;
    }

    private ActorContext actorContext() {
        return ActorContext.member(1001L, ActorRole.USER, "public-image-test");
    }
}
