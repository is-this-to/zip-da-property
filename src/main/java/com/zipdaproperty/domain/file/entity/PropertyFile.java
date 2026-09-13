package com.zipdaproperty.domain.file.entity;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "file_purpose",
            nullable = false,
            updatable = false,
            length = 30
    )
    private FilePurpose filePurpose;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "upload_status",
            nullable = false,
            length = 30
    )
    private UploadStatus uploadStatus;

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
            name = "mime_type",
            length = 100
    )
    private String mimeType;

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

    @Column(
            name = "linked_at",
            columnDefinition = "DATETIME(6)"
    )
    private Instant linkedAt;

    @Column(
            name = "object_deleted_at",
            columnDefinition = "DATETIME(6)"
    )
    private Instant objectDeletedAt;

    private PropertyFile(
            Long propertyFileId,
            String uploadSessionId,
            FilePurpose filePurpose,
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
        this.filePurpose = filePurpose;
        this.uploadStatus = UploadStatus.CREATED;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.objectKey = objectKey;
        this.expiresAt = expiresAt;
    }

    public static PropertyFile create(
            Long propertyFileId,
            String uploadSessionId,
            FilePurpose filePurpose,
            String originalFileName,
            Long fileSize,
            String objectKey,
            Instant expiresAt,
            ActorContext actorContext
    ) {
        return new PropertyFile(
                propertyFileId,
                uploadSessionId,
                filePurpose,
                originalFileName,
                fileSize,
                objectKey,
                expiresAt,
                actorContext
        );
    }

    public void complete(
            String checksum,
            String mimeType,
            ActorContext actorContext
    ) {
        this.checksum = checksum;
        this.mimeType = mimeType;
        this.uploadStatus = UploadStatus.VERIFIED;
        recordUpdate(actorContext);
    }

    public boolean isVerificationCompleted() {
        return uploadStatus == UploadStatus.VERIFIED
                || uploadStatus == UploadStatus.LINKED;
    }

    public boolean isReadyToLink() {
        return uploadStatus == UploadStatus.VERIFIED;
    }

    public void markLinked(
            ActorContext actorContext
    ) {
        if (!isReadyToLink()) {
            throw new IllegalStateException(
                    "VERIFIED 상태의 파일만 LINKED 상태로 변경할 수 있습니다."
            );
        }

        this.uploadStatus = UploadStatus.LINKED;
        this.linkedAt = Instant.now();
        recordUpdate(actorContext);
    }

    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ) {
        recordDeletion(
                actorContext,
                deletedAt,
                deleteReason
        );
    }

    public void markObjectDeleted(
            Instant objectDeletedAt,
            ActorContext actorContext
    ) {
        if (this.objectDeletedAt != null) {
            return;
        }

        this.objectDeletedAt = objectDeletedAt;
        recordUpdate(actorContext);
    }
}
