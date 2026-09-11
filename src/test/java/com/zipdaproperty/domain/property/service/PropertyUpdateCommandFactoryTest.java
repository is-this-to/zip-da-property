package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.request.PropertyUpdateRequest;
import com.zipdaproperty.domain.property.request.PropertyOptionRequest;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PropertyUpdateCommandFactoryTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private final PropertyUpdateCommandFactory factory =
            new PropertyUpdateCommandFactory(objectMapper);

    @Test
    void create_emptyChanges_allowsImageOnlyRequest() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(),
                List.of(101L, 102L, 103L)
        );

        PropertyUpdateCommand command = factory.create(
                mock(Property.class),
                request
        );

        assertThat(command.requestedVersion()).isEqualTo(3L);
    }

    @Test
    void create_propertyChanges_keepsFactoryResponsibility() {
        JsonNode titleNode = objectMapper.readTree("\"수정 제목\"");
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of("title", titleNode)
        );

        PropertyUpdateCommand command = factory.create(
                mock(Property.class),
                request
        );

        assertThat(command.title()).isEqualTo("수정 제목");
    }

    @Test
    void create_optionsOnly_acceptsEmptyAndNonEmptyLists() {
        for (List<PropertyOptionRequest> options : List.of(
                List.<PropertyOptionRequest>of(), List.of(new PropertyOptionRequest("PARKING", "2")))) {
            PropertyUpdateCommand command = factory.create(mock(Property.class),
                    new PropertyUpdateRequest(3L, null, null, null, options));
            assertThat(command.requestedVersion()).isEqualTo(3L);
        }
    }

    @Test
    void create_noTargets_rejects() {
        assertThatThrownBy(() -> factory.create(mock(Property.class), new PropertyUpdateRequest(3L, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.INVALID_REQUEST));
    }

    @Test
    void create_invalidChangesWithOptions_stillRejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(3L,
                Map.of("options", objectMapper.readTree("[]")), null, null, List.of());
        assertThatThrownBy(() -> factory.create(mock(Property.class), request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode()).isEqualTo(CustomResponseCode.INVALID_REQUEST));
    }

    @Test
    void create_fileIdsInsideChanges_rejects() {
        PropertyUpdateRequest request = new PropertyUpdateRequest(
                3L,
                Map.of(
                        "fileIds",
                        objectMapper.readTree("[101, 102]")
                )
        );

        assertThatThrownBy(() -> factory.create(
                mock(Property.class),
                request
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );
    }
}
