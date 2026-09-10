package com.zipdaproperty.domain.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Validated
@ConfigurationProperties(prefix = "minio")
public record MinioImageProperties(

        @NotEmpty
        List<@NotBlank String> allowImageExtensions
) {

    public MinioImageProperties {
        if (allowImageExtensions != null) {
            allowImageExtensions = List.copyOf(allowImageExtensions);
        }
    }

    public boolean allowsFileExtension(String extension) {
        return findMimeTypeForFileExtension(extension).isPresent();
    }

    public Optional<String> findMimeTypeForFileExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        Optional<String> exactMimeType = normalizedMimeTypes()
                .filter(mimeType -> extractImageSubtype(mimeType)
                        .equals(normalizedExtension))
                .findFirst();
        if (exactMimeType.isPresent()) {
            return exactMimeType;
        }

        return normalizedMimeTypes()
                .filter(mimeType -> matchesExtension(
                        extractImageSubtype(mimeType),
                        normalizedExtension
                ))
                .findFirst();
    }

    private Stream<String> normalizedMimeTypes() {
        return allowImageExtensions.stream()
                .map(String::trim)
                .map(mimeType -> mimeType.toLowerCase(Locale.ROOT));
    }

    private static String extractImageSubtype(String mimeType) {
        String normalizedMimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        String imagePrefix = "image/";
        if (!normalizedMimeType.startsWith(imagePrefix)) {
            return "";
        }
        return normalizedMimeType.substring(imagePrefix.length());
    }

    private static boolean matchesExtension(
            String subtype,
            String extension
    ) {
        if ("jpeg".equals(subtype) || "jpg".equals(subtype)) {
            return "jpeg".equals(extension) || "jpg".equals(extension);
        }
        return subtype.equals(extension);
    }
}
