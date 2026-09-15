package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PropertyPublicationStatusEntityTest {

    private static final Long PROPERTY_ID = 884685586571263701L;

    private final ActorContext actorContext =
            ActorContext.member(
                    1001L,
                    ActorRole.CS_ADMIN,
                    "publication-entity-test"
            );

    @Test
    void changePublicationStatus_publishedSetsPublishedAt() {
        Property property = createProperty();
        Instant publishedAt = Instant.parse("2026-09-11T02:00:00Z");

        property.changePublicationStatus(
                PublicationStatus.PUBLISHED,
                actorContext,
                publishedAt
        );

        assertThat(property.getPublicationStatus())
                .isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(property.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void changePublicationStatus_hiddenKeepsLastPublishedAt() {
        Property property = createProperty();
        Instant publishedAt = Instant.parse("2026-09-11T02:00:00Z");

        property.changePublicationStatus(
                PublicationStatus.PUBLISHED,
                actorContext,
                publishedAt
        );
        property.changePublicationStatus(
                PublicationStatus.HIDDEN,
                actorContext,
                Instant.parse("2026-09-11T03:00:00Z")
        );

        assertThat(property.getPublicationStatus())
                .isEqualTo(PublicationStatus.HIDDEN);
        assertThat(property.getPublishedAt()).isEqualTo(publishedAt);
    }

    private Property createProperty() {
        return Property.create(
                PROPERTY_ID,
                mock(PropertyCreateCommand.class),
                actorContext
        );
    }
}
