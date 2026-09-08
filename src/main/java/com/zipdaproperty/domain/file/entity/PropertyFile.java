package com.zipdaproperty.domain.file.entity;

import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "property_file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyFile extends BaseAuditEntity {

    @Id
    @Column(
            name = "property_file_id",
            nullable = false,
            updatable = false
    )
    private Long propertyFileId;

    @Column(
            name = "upload_session_id",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String uploadSessionId;

    @Column(
            name = "owner_member_id",
            nullable = false,
            updatable = false
    )
    private Long ownerMemberId;

    @Column(
            name = "original_file_name",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String originalFileName;

    @Column(
            name = "file_size",
            nullable = false,
            updatable = false
    )
    private Long fileSize;

    @Column(
            name = "object_key",
            nullable = false,
            updatable = false,
            unique = true,
            length = 500
    )
    private String objectKey;

    @Column(
            name = "checksum",
            length = 64
    )
    private String checksum;

    @Column(
            name = "expires_at",
            nullable = false,
            updatable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant expiresAt;

    private PropertyFile(
            Long propertyFileId,
            String uploadSessionId,
            String originalFileName,
            Long fileSize,
            String objectKey,
            Instant expiresAt,
            ActorContext actorContext
    ) {
        super(actorContext);
        this.propertyFileId = propertyFileId;
        this.uploadSessionId = uploadSessionId;
        this.ownerMemberId = actorContext.memberId();
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.objectKey = objectKey;
        this.expiresAt = expiresAt;
    }

    public static PropertyFile create(
            Long propertyFileId,
            String uploadSessionId,
            String originalFileName,
            Long fileSize,
            String objectKey,
            Instant expiresAt,
            ActorContext actorContext
    ) {
        return new PropertyFile(
                propertyFileId,
                uploadSessionId,
                originalFileName,
                fileSize,
                objectKey,
                expiresAt,
                actorContext
        );
    }

    public void complete(
            String checksum,
            ActorContext actorContext
    ) {
        this.checksum = checksum;
        recordUpdate(actorContext);
    }
}
