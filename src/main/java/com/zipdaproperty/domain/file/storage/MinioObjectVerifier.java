package com.zipdaproperty.domain.file.storage;

import com.zipdaproperty.domain.file.constant.FileUploadPolicy;
import com.zipdaproperty.domain.file.constant.ImageFileType;
import com.zipdaproperty.global.error.custom.business.FileManagedException;
import com.zipdaproperty.global.error.custom.business.FileTooLargeException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class MinioObjectVerifier {

    private static final int MAGIC_HEADER_LENGTH = 12;

    private final MinioClient minioClient;

    @Value("${minio.minio-bucket}")
    private String bucket;

    public MinioObjectVerification verify(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            validateObjectSize(stat.size());

            try (GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            )) {
                return inspectStream(stream, stat.size());
            }
        } catch (FileTooLargeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileManagedException(
                    "업로드된 파일을 확인할 수 없습니다."
            );
        }
    }

    private void validateObjectSize(long objectSize) {
        if (objectSize > FileUploadPolicy.MAX_FILE_SIZE_BYTES) {
            throw new FileTooLargeException(
                    "업로드된 파일 크기는 20MB 이하여야 합니다."
            );
        }
    }

    MinioObjectVerification inspectStream(
            InputStream stream,
            long objectSize
    ) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] magicHeader = new byte[MAGIC_HEADER_LENGTH];
        int magicHeaderSize = 0;
        byte[] buffer = new byte[8192];
        int readSize;

        while ((readSize = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, readSize);
            if (magicHeaderSize < MAGIC_HEADER_LENGTH) {
                int copySize = Math.min(
                        readSize,
                        MAGIC_HEADER_LENGTH - magicHeaderSize
                );
                System.arraycopy(
                        buffer,
                        0,
                        magicHeader,
                        magicHeaderSize,
                        copySize
                );
                magicHeaderSize += copySize;
            }
        }

        return new MinioObjectVerification(
                objectSize,
                HexFormat.of().formatHex(digest.digest()),
                detectImageFileType(magicHeader, magicHeaderSize)
        );
    }

    private ImageFileType detectImageFileType(
            byte[] header,
            int headerSize
    ) {
        if (headerSize >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF) {
            return ImageFileType.JPEG;
        }

        if (headerSize >= 8
                && unsigned(header[0]) == 0x89
                && unsigned(header[1]) == 0x50
                && unsigned(header[2]) == 0x4E
                && unsigned(header[3]) == 0x47
                && unsigned(header[4]) == 0x0D
                && unsigned(header[5]) == 0x0A
                && unsigned(header[6]) == 0x1A
                && unsigned(header[7]) == 0x0A) {
            return ImageFileType.PNG;
        }

        if (headerSize >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8'
                && (header[4] == '7' || header[4] == '9')
                && header[5] == 'a') {
            return ImageFileType.GIF;
        }

        if (headerSize >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return ImageFileType.WEBP;
        }

        return ImageFileType.UNKNOWN;
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
