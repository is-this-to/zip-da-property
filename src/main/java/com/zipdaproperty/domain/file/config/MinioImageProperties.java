package com.zipdaproperty.domain.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

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
        if (extension == null) {
            return false;
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        return allowImageExtensions.stream()
                .map(MinioImageProperties::extractImageSubtype)
                .anyMatch(subtype -> matchesExtension(
                        subtype,
                        normalizedExtension
                ));
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
