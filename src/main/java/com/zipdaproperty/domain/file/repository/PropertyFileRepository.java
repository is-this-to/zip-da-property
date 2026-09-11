package com.zipdaproperty.domain.file.repository;

import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyFileRepository extends JpaRepository<PropertyFile, Long> {

    Optional<PropertyFile> findByPropertyFileIdAndDeletedAtIsNull(
            Long propertyFileId
    );

    List<PropertyFile> findAllByPropertyFileIdInAndDeletedAtIsNull(
            Collection<Long> propertyFileIds
    );

    List<PropertyFile> findAllByDeletedAtIsNotNullAndObjectDeletedAtIsNull();

    @Query("""
            select propertyFile
            from PropertyFile propertyFile
            where propertyFile.uploadStatus = :uploadStatus
              and propertyFile.updatedAt <= :verifiedBefore
              and propertyFile.deletedAt is null
              and not exists (
                  select propertyImage.propertyImageId
                  from PropertyImage propertyImage
                  where propertyImage.propertyFileId = propertyFile.propertyFileId
                    and propertyImage.deletedAt is null
              )
            """)
    List<PropertyFile> findUnlinkedFilesVerifiedBefore(
            @Param("uploadStatus") UploadStatus uploadStatus,
            @Param("verifiedBefore") Instant verifiedBefore
    );
}
