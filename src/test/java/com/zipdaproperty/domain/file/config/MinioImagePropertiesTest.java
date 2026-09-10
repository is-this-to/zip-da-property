package com.zipdaproperty.domain.file.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MinioImagePropertiesTest {

    @Test
    void bind_applicationYaml_usesConfiguredImageMimeTypes()
            throws IOException {
        List<PropertySource<?>> propertySources =
                new YamlPropertySourceLoader().load(
                        "application",
                        new ClassPathResource("application.yaml")
                );
        Binder binder = new Binder(
                ConfigurationPropertySources.from(propertySources)
        );

        MinioImageProperties properties = binder.bind(
                "minio",
                Bindable.of(MinioImageProperties.class)
        ).orElseThrow(() -> new IllegalStateException(
                "MinIO 이미지 설정을 바인딩할 수 없습니다."
        ));

        assertThat(List.of("jpg", "jpeg", "png", "gif", "webp"))
                .allMatch(properties::allowsFileExtension);
        assertThat(properties.findMimeTypeForFileExtension("jpg"))
                .contains("image/jpg");
        assertThat(properties.findMimeTypeForFileExtension("jpeg"))
                .contains("image/jpeg");
        assertThat(properties.findMimeTypeForFileExtension("png"))
                .contains("image/png");
        assertThat(properties.findMimeTypeForFileExtension("gif"))
                .contains("image/gif");
        assertThat(properties.findMimeTypeForFileExtension("webp"))
                .contains("image/webp");
        assertThat(properties.allowsFileExtension("bmp")).isFalse();
    }
}
