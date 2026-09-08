package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.exception.FileTooLargeException;
import com.zipdaproperty.domain.file.exception.InvalidFileTypeException;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.UploadFileRequest;
import com.zipdaproperty.domain.file.request.UploadSessionCreateRequest;
import com.zipdaproperty.domain.file.response.UploadSessionCreateResponse;
import com.zipdaproperty.domain.file.response.UploadSessionFileResponse;
import com.zipdaproperty.domain.file.storage.MinioPresignedUploadUrlGenerator;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.id.TsidGenerator;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadSessionService {

    static final Duration UPLOAD_SESSION_TTL = Duration.ofMinutes(15);
    static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    static final int MAX_FILE_COUNT = 30;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private final PropertyFileRepository propertyFileRepository;
    private final TsidGenerator tsidGenerator;
    private final MinioPresignedUploadUrlGenerator uploadUrlGenerator;

    @Value("${minio.minio-image-path}")
    private String minioImagePath;

    @Transactional
    public UploadSessionCreateResponse create(
            UploadSessionCreateRequest request,
            ActorContext actorContext
    ) {
        List<UploadFileRequest> requestedFiles = validateRequest(request);
        String uploadSessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(UPLOAD_SESSION_TTL);

        List<PropertyFile> propertyFiles = new ArrayList<>(requestedFiles.size());
        for (UploadFileRequest requestedFile : requestedFiles) {
            if (requestedFile == null) {
                throw new BusinessException(
                        CustomResponseCode.INVALID_REQUEST,
                        "파일 정보는 필수입니다."
                );
            }
            String originalFileName = sanitizeFileName(requestedFile.name());
            String extension = validateAndGetExtension(originalFileName);
            validateFileSize(requestedFile.size());

            Long fileId = tsidGenerator.generate();
            String objectKey = createObjectKey(
                    uploadSessionId,
                    fileId,
                    extension
            );
            propertyFiles.add(PropertyFile.create(
                    fileId,
                    uploadSessionId,
                    originalFileName,
                    requestedFile.size(),
                    objectKey,
                    expiresAt,
                    actorContext
            ));
        }

        List<PropertyFile> savedFiles = propertyFileRepository.saveAll(propertyFiles);
        List<UploadSessionFileResponse> fileResponses = savedFiles.stream()
                .map(file -> new UploadSessionFileResponse(
                        file.getPropertyFileId(),
                        uploadUrlGenerator.generate(
                                file.getObjectKey(),
                                Duration.between(
                                        Instant.now(),
                                        expiresAt
                                )
                        ),
                        Map.of()
                ))
                .toList();

        return new UploadSessionCreateResponse(
                uploadSessionId,
                expiresAt,
                fileResponses
        );
    }

    private List<UploadFileRequest> validateRequest(
            UploadSessionCreateRequest request
    ) {
        if (request == null
                || request.files() == null
                || request.files().isEmpty()
                || request.files().size() > MAX_FILE_COUNT) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "파일은 1개 이상 30개 이하로 요청해야 합니다."
            );
        }
        return request.files();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "파일명은 필수입니다."
            );
        }

        String normalized = fileName.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (basename.isBlank() || basename.length() > 255) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "유효한 파일명을 입력해야 합니다."
            );
        }
        return basename;
    }

    private String validateAndGetExtension(String fileName) {
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator < 1 || extensionSeparator == fileName.length() - 1) {
            throw new InvalidFileTypeException(
                    "허용되지 않는 파일 형식입니다."
            );
        }

        String extension = fileName.substring(extensionSeparator + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileTypeException(
                    "JPEG, PNG, WebP 파일만 업로드할 수 있습니다."
            );
        }
        return extension;
    }

    private void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "파일 크기는 0보다 커야 합니다."
            );
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException(
                    "파일 크기는 20MB 이하여야 합니다."
            );
        }
    }

    private String createObjectKey(
            String uploadSessionId,
            Long fileId,
            String extension
    ) {
        String prefix = minioImagePath == null
                ? ""
                : minioImagePath.replace('\\', '/').replaceAll("^/+|/+$", "");
        String generatedPath = "property-files/"
                + uploadSessionId
                + "/"
                + fileId
                + "."
                + extension;
        return prefix.isBlank()
                ? generatedPath
                : prefix + "/" + generatedPath;
    }
}
