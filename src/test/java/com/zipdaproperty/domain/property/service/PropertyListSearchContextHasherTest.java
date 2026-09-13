package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.model.PropertyListBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyListSearchContextHasherTest {

    private final PropertyListSearchContextHasher hasher =
            new PropertyListSearchContextHasher();

    @Test
    void setOrderDoesNotChangeHash() {
        PropertyMapSearchCondition first = condition(
                new LinkedHashSet<>(Set.of(PropertyType.APARTMENT, PropertyType.ROOM))
        );
        PropertyMapSearchCondition second = condition(
                new LinkedHashSet<>(Set.of(PropertyType.ROOM, PropertyType.APARTMENT))
        );
        PropertyListBounds bounds = new PropertyListBounds(37.4, 37.6, 126.8, 127.2);

        assertThat(hasher.hash(bounds, first))
                .isEqualTo(hasher.hash(bounds, second));
    }

    @Test
    void boundsChangeChangesHash() {
        PropertyMapSearchCondition condition = condition(Set.of(PropertyType.APARTMENT));

        assertThat(hasher.hash(
                new PropertyListBounds(37.4, 37.6, 126.8, 127.2),
                condition
        )).isNotEqualTo(hasher.hash(
                new PropertyListBounds(37.4, 37.7, 126.8, 127.2),
                condition
        ));
    }

    private PropertyMapSearchCondition condition(Set<PropertyType> propertyTypes) {
        return new PropertyMapSearchCondition(
                propertyTypes, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                PropertyMapSort.LATEST
        );
    }
}
