package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.storage.MinioPresignedGetUrlGenerator;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.image.repository.PropertyImageRepository;
import com.zipdaproperty.domain.property.response.PropertyPublicDetailImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyPublicImageService {

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyFileRepository propertyFileRepository;
    private final MinioPresignedGetUrlGenerator getUrlGenerator;

    public List<PropertyPublicDetailImageResponse> findImages(Long propertyId) {
        List<PropertyImage> images = propertyImageRepository
                .findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(propertyId);
        if (images.isEmpty()) {
            return List.of();
        }

        List<Long> fileIds = images.stream()
                .map(PropertyImage::getPropertyFileId)
                .toList();
        Map<Long, PropertyFile> activeFiles = propertyFileRepository
                .findAllByPropertyFileIdInAndDeletedAtIsNull(fileIds)
                .stream()
                .collect(Collectors.toMap(
                        PropertyFile::getPropertyFileId,
                        Function.identity()
                ));

        return images.stream()
                .filter(image -> isAvailableImageFile(
                        activeFiles.get(image.getPropertyFileId())
                ))
                .map(image -> toResponse(
                        image,
                        activeFiles.get(image.getPropertyFileId())
                ))
                .toList();
    }

    private boolean isAvailableImageFile(PropertyFile propertyFile) {
        return propertyFile != null
                && propertyFile.getUploadStatus() == UploadStatus.LINKED
                && propertyFile.getFilePurpose() == FilePurpose.PROPERTY_IMAGE
                && propertyFile.getObjectDeletedAt() == null;
    }

    private PropertyPublicDetailImageResponse toResponse(
            PropertyImage image,
            PropertyFile propertyFile
    ) {
        return new PropertyPublicDetailImageResponse(
                image.getPropertyFileId(),
                getUrlGenerator.generate(propertyFile.getObjectKey()),
                image.getSortOrder(),
                image.getIsRepresentative()
        );
    }
}
