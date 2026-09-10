package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.FileUploadPolicy;
import com.zipdaproperty.domain.file.constant.ImageFileType;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.file.request.PropertyFileCompleteRequest;
import com.zipdaproperty.domain.file.response.PropertyFileCompleteResponse;
import com.zipdaproperty.domain.file.storage.MinioObjectVerification;
import com.zipdaproperty.domain.file.storage.MinioObjectVerifier;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.FileTooLargeException;
import com.zipdaproperty.global.error.custom.business.InvalidFileTypeException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.error.custom.business.UploadSessionExpiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PropertyFileCompleteService {

    private static final Pattern SHA_256_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{64}$");

    private final PropertyFileRepository propertyFileRepository;
    private final MinioObjectVerifier minioObjectVerifier;

    @Transactional
    public PropertyFileCompleteResponse complete(
            Long fileId,
            PropertyFileCompleteRequest request,
            ActorContext actorContext
    ) {
        PropertyFile propertyFile = propertyFileRepository
                .findByPropertyFileIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new NotFoundResourceException(
                        "파일을 찾을 수 없습니다."
                ));

        validateOwnership(propertyFile, actorContext);
        validateExpiration(propertyFile);

        String normalizedChecksum = validateAndNormalizeChecksum(request);
        validateDeclaredSize(request.size(), propertyFile.getFileSize());

        MinioObjectVerification verification =
                minioObjectVerifier.verify(propertyFile.getObjectKey());

        validateActualSize(verification.size(), request.size());
        validateImageFileType(
                propertyFile.getOriginalFileName(),
                verification.imageFileType()
        );
        validateChecksum(
                normalizedChecksum,
                verification.checksum()
        );

        propertyFile.complete(normalizedChecksum, actorContext);
        propertyFileRepository.save(propertyFile);

        return new PropertyFileCompleteResponse();
    }

    private void validateOwnership(
            PropertyFile propertyFile,
            ActorContext actorContext
    ) {
        if (!propertyFile.getOwnerMemberId().equals(actorContext.memberId())) {
            throw new FileOwnershipRequiredException(
                    "파일 소유자만 업로드를 완료할 수 있습니다."
            );
        }
    }

    private void validateExpiration(PropertyFile propertyFile) {
        if (propertyFile.getExpiresAt().isBefore(Instant.now())) {
            throw new UploadSessionExpiredException(
                    "업로드 세션이 만료되었습니다."
            );
        }
    }

    private String validateAndNormalizeChecksum(
            PropertyFileCompleteRequest request
    ) {
        if (request == null
                || request.checksum() == null
                || !SHA_256_PATTERN.matcher(request.checksum()).matches()) {
            throw invalidRequest(
                    "체크섬은 64자리 SHA-256 hexadecimal 문자열이어야 합니다."
            );
        }
        return request.checksum().toLowerCase(Locale.ROOT);
    }

    private void validateDeclaredSize(
            Long requestSize,
            Long declaredSize
    ) {
        if (requestSize == null || requestSize <= 0) {
            throw invalidRequest("파일 크기는 0보다 커야 합니다.");
        }
        if (requestSize > FileUploadPolicy.MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException(
                    "파일 크기는 20MB 이하여야 합니다."
            );
        }
        if (!requestSize.equals(declaredSize)) {
            throw invalidRequest(
                    "요청한 파일 크기가 업로드 세션의 선언값과 일치하지 않습니다."
            );
        }
    }

    private void validateActualSize(
            long actualSize,
            long requestSize
    ) {
        if (actualSize != requestSize) {
            throw invalidRequest(
                    "업로드된 파일 크기가 요청값과 일치하지 않습니다."
            );
        }
    }

    private void validateImageFileType(
            String originalFileName,
            ImageFileType actualImageFileType
    ) {
        ImageFileType expectedImageFileType = expectedImageFileType(
                originalFileName
        );
        if (expectedImageFileType == ImageFileType.UNKNOWN
                || expectedImageFileType != actualImageFileType) {
            throw new InvalidFileTypeException(
                    "파일 확장자와 실제 이미지 형식이 일치하지 않습니다."
            );
        }
    }

    private ImageFileType expectedImageFileType(String originalFileName) {
        int separator = originalFileName.lastIndexOf('.');
        if (separator < 0 || separator == originalFileName.length() - 1) {
            return ImageFileType.UNKNOWN;
        }

        return switch (originalFileName.substring(separator + 1)
                .toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> ImageFileType.JPEG;
            case "png" -> ImageFileType.PNG;
            case "gif" -> ImageFileType.GIF;
            case "webp" -> ImageFileType.WEBP;
            default -> ImageFileType.UNKNOWN;
        };
    }

    private void validateChecksum(
            String requestedChecksum,
            String actualChecksum
    ) {
        if (!requestedChecksum.equalsIgnoreCase(actualChecksum)) {
            throw invalidRequest(
                    "요청한 체크섬이 업로드된 파일과 일치하지 않습니다."
            );
        }
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}
