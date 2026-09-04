package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyOptionQueryDSLRepository;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeListResponse;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeResponse;
import com.zipdaproperty.domain.property.constant.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PropertyOptionQueryServiceTest {

    private final PropertyOptionQueryDSLRepository queryRepository = mock(PropertyOptionQueryDSLRepository.class);
    private final PropertyOptionQueryService service =
            new PropertyOptionQueryService(queryRepository);

    @Test
    void getOptionCodes_noMappings_skipsCodeQuery() {
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of());

        assertThat(service.getOptionCodes(PropertyType.ROOM).items()).isEmpty();
        verify(queryRepository, never()).findActiveOptionCodesByIds(anyCollection());
    }

    @Test
    void getOptionCodes_unorderedMappings_sortsAndQueriesCodesOnce() {
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(mapping(3, 30, 20), mapping(2, 20, 10), mapping(1, 10, 10)));
        when(queryRepository.findActiveOptionCodesByIds(List.of(30L, 20L, 10L)))
                .thenReturn(List.of(code(20, "B"), code(30, "C"), code(10, "A")));

        var items = service.getOptionCodes(PropertyType.ROOM).items();

        assertThat(items).extracting(PropertyOptionCodeResponse::optionCode).containsExactly("A", "B", "C");
        // 등록·검색 capability가 false여도 공용 메타데이터에서는 제외하지 않는다.
        assertThat(items).allMatch(item -> !item.filterable() && !item.registrationEnabled());
        verify(queryRepository).findActiveOptionCodesByIds(List.of(30L, 20L, 10L));
        verify(queryRepository).findActiveTypeOptions(PropertyType.ROOM);
        verifyNoMoreInteractions(queryRepository);
    }

    @Test
    void getOptionCodes_codeNotReturned_excludesMapping() {
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(mapping(1, 10, 10), mapping(2, 20, 20)));
        when(queryRepository.findActiveOptionCodesByIds(List.of(10L, 20L)))
                .thenReturn(List.of(code(10, "A")));

        assertThat(service.getOptionCodes(PropertyType.ROOM).items())
                .extracting(PropertyOptionCodeResponse::optionCode).containsExactly("A");
    }

    @Test
    void getOptionCodes_duplicateMappings_preservesRowsAndDeduplicatesQueryIds() {
        PropertyTypeOption first = mapping(1, 10, 10);
        ReflectionTestUtils.setField(first, "required", true);
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(mapping(2, 10, 10), first));
        when(queryRepository.findActiveOptionCodesByIds(List.of(10L)))
                .thenReturn(List.of(code(10, "A")));

        assertThat(service.getOptionCodes(PropertyType.ROOM).items())
                .extracting(PropertyOptionCodeResponse::required).containsExactly(true, false);
        verify(queryRepository).findActiveOptionCodesByIds(List.of(10L));
        verify(queryRepository).findActiveTypeOptions(PropertyType.ROOM);
        verifyNoMoreInteractions(queryRepository);
    }

    @Test
    void listResponse_mutableInput_copiesList() {
        var source = new ArrayList<PropertyOptionCodeResponse>();
        source.add(new PropertyOptionCodeResponse("A", "옵션", null, false, true, false, 10));
        var response = new PropertyOptionCodeListResponse(source);
        source.clear();

        assertThat(response.items()).hasSize(1);
        assertThatThrownBy(() -> response.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private PropertyTypeOption mapping(long id, long codeId, int order) {
        PropertyTypeOption mapping = BeanUtils.instantiateClass(PropertyTypeOption.class);
        ReflectionTestUtils.setField(mapping, "propertyTypeOptionId", id);
        ReflectionTestUtils.setField(mapping, "optionCodeId", codeId);
        ReflectionTestUtils.setField(mapping, "displayOrder", order);
        return mapping;
    }

    private PropertyOptionCode code(long id, String code) {
        PropertyOptionCode entity = BeanUtils.instantiateClass(PropertyOptionCode.class);
        ReflectionTestUtils.setField(entity, "optionCodeId", id);
        ReflectionTestUtils.setField(entity, "optionCode", code);
        ReflectionTestUtils.setField(entity, "optionName", "옵션");
        return entity;
    }
}
