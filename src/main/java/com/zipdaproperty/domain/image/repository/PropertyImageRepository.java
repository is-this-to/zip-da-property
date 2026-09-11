package com.zipdaproperty.domain.image.repository;

import com.zipdaproperty.domain.image.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyImageRepository
        extends JpaRepository<PropertyImage, Long> {

    boolean existsByPropertyFileIdAndDeletedAtIsNull(
            Long propertyFileId
    );

    boolean existsByPropertyIdAndPropertyFileIdAndDeletedAtIsNull(
            Long propertyId,
            Long propertyFileId
    );

    List<PropertyImage>
    findAllByPropertyIdAndDeletedAtIsNullOrderBySortOrderAsc(
            Long propertyId
    );
}
