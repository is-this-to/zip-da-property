package com.zipdaproperty.domain.property.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property.list.cursor")
public record PropertyListCursorProperties(
        String secret,
        String secretVersion
) {
}
