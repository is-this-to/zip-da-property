package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyOptionQueryDSLRepository;
import com.zipdaproperty.domain.option.repository.PropertyOptionRepository;
import com.zipdaproperty.domain.option.validator.OptionValueValidator;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.OptionCodeNotFoundException;
import com.zipdaproperty.global.error.custom.business.OptionNotAllowedForPropertyTypeException;
import com.zipdaproperty.global.error.custom.business.OptionValueInvalidException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyOptionCommandServiceTest {

    private final PropertyOptionRepository optionRepository = mock(PropertyOptionRepository.class);
    private final PropertyOptionQueryDSLRepository queryRepository = mock(PropertyOptionQueryDSLRepository.class);
    private final OptionValueValidator valueValidator = mock(OptionValueValidator.class);
    private final PropertyOptionCommandService service = new PropertyOptionCommandService(
            optionRepository,
            queryRepository,
            valueValidator
    );

    private final ActorContext actorContext = ActorContext.system("option-create-test");

    @BeforeEach
    void setUp() {
        when(valueValidator.isValid("true")).thenReturn(true);
        when(valueValidator.isValid("false")).thenReturn(true);
    }

    @Test
    void createOptions_validOptions_savesAll() {
        PropertyOptionCode airConditioner = optionCode(10L, "AIR_CONDITIONER", true);
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        PropertyTypeOption airConditionerMapping = typeOption(10L, 1);
        PropertyTypeOption bedMapping = typeOption(20L, 2);

        when(queryRepository.findActiveOptionCodesByCodes(List.of("AIR_CONDITIONER", "BED")))
                .thenReturn(List.of(airConditioner, bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(airConditionerMapping, bedMapping));
        when(queryRepository.findActiveOptionsByPropertyId(100L)).thenReturn(List.of());

        service.createOptions(
                100L,
                PropertyType.ROOM,
                List.of(
                        new PropertyOptionCreateCommand("AIR_CONDITIONER", "true"),
                        new PropertyOptionCreateCommand("BED", "false")
                ),
                actorContext
        );

        verify(optionRepository).saveAll(anyList());
    }

    @Test
    void createOptions_duplicateCodeInRequest_rejectsBeforeQuery() {
        List<PropertyOptionCreateCommand> commands = List.of(
                new PropertyOptionCreateCommand("BED", "true"),
                new PropertyOptionCreateCommand("BED", "false")
        );

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.ROOM,
                commands,
                actorContext
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
                );

        verify(optionRepository, never()).saveAll(anyList());
    }

    @Test
    void createOptions_invalidValue_rejects() {
        when(valueValidator.isValid("TRUE")).thenReturn(false);

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.ROOM,
                List.of(new PropertyOptionCreateCommand("BED", "TRUE")),
                actorContext
        )).isInstanceOf(OptionValueInvalidException.class);
    }

    @Test
    void createOptions_missingOrInactiveCode_rejects() {
        when(queryRepository.findActiveOptionCodesByCodes(List.of("BED")))
                .thenReturn(List.of());
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of());
        when(queryRepository.findActiveOptionsByPropertyId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.ROOM,
                List.of(new PropertyOptionCreateCommand("BED", "true")),
                actorContext
        )).isInstanceOf(OptionCodeNotFoundException.class);
    }

    @Test
    void createOptions_notAllowedForPropertyType_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        when(queryRepository.findActiveOptionCodesByCodes(List.of("BED")))
                .thenReturn(List.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.APARTMENT))
                .thenReturn(List.of());
        when(queryRepository.findActiveOptionsByPropertyId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.APARTMENT,
                List.of(new PropertyOptionCreateCommand("BED", "true")),
                actorContext
        )).isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void createOptions_registrationDisabled_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", false);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        when(queryRepository.findActiveOptionCodesByCodes(List.of("BED")))
                .thenReturn(List.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.ROOM,
                List.of(new PropertyOptionCreateCommand("BED", "true")),
                actorContext
        )).isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void createOptions_activeDuplicate_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        when(queryRepository.findActiveOptionCodesByCodes(List.of("BED")))
                .thenReturn(List.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyId(100L))
                .thenReturn(List.of(new PropertyOption(100L, 20L, "false", 1, actorContext)));

        assertThatThrownBy(() -> service.createOptions(
                100L,
                PropertyType.ROOM,
                List.of(new PropertyOptionCreateCommand("BED", "true")),
                actorContext
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
                );

        verify(optionRepository, never()).saveAll(anyList());
    }

    @Test
    void changeOptionValue_validOption_changesAndSaves() {
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        PropertyOption propertyOption =
                new PropertyOption(100L, 20L, "true", 1, actorContext);
        when(queryRepository.findActiveOptionCode("BED"))
                .thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(100L, 20L))
                .thenReturn(List.of(propertyOption));

        service.changeOptionValue(
                100L,
                PropertyType.ROOM,
                "BED",
                "false",
                actorContext
        );

        assertThat(propertyOption.getOptionValue()).isEqualTo("false");
        verify(optionRepository).save(propertyOption);
    }

    @Test
    void changeOptionValue_invalidValue_rejectsBeforeQuery() {
        when(valueValidator.isValid("TRUE")).thenReturn(false);

        assertThatThrownBy(() -> service.changeOptionValue(
                100L,
                PropertyType.ROOM,
                "BED",
                "TRUE",
                actorContext
        )).isInstanceOf(OptionValueInvalidException.class);

        verify(optionRepository, never()).save(any(PropertyOption.class));
    }

    @Test
    void changeOptionValue_inactiveOrDeletedCode_rejects() {
        when(queryRepository.findActiveOptionCode("BED")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeOptionValue(
                100L,
                PropertyType.ROOM,
                "BED",
                "true",
                actorContext
        )).isInstanceOf(OptionCodeNotFoundException.class);
    }

    @Test
    void changeOptionValue_notAllowedForPropertyType_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        when(queryRepository.findActiveOptionCode("BED")).thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.APARTMENT))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.changeOptionValue(
                100L,
                PropertyType.APARTMENT,
                "BED",
                "true",
                actorContext
        )).isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void changeOptionValue_registrationDisabled_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", false);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        when(queryRepository.findActiveOptionCode("BED")).thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));

        assertThatThrownBy(() -> service.changeOptionValue(
                100L,
                PropertyType.ROOM,
                "BED",
                "true",
                actorContext
        )).isInstanceOf(OptionNotAllowedForPropertyTypeException.class);
    }

    @Test
    void changeOptionValue_noActivePropertyOption_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", true);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        when(queryRepository.findActiveOptionCode("BED")).thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(100L, 20L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.changeOptionValue(
                100L,
                PropertyType.ROOM,
                "BED",
                "true",
                actorContext
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
                );
    }

    @Test
    void softDeleteOption_registrationDisabled_deletesActiveOption() {
        PropertyOptionCode bed = optionCode(20L, "BED", false);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        PropertyOption propertyOption =
                new PropertyOption(100L, 20L, "true", 1, actorContext);
        when(queryRepository.findActiveOptionCode("BED"))
                .thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(100L, 20L))
                .thenReturn(List.of(propertyOption));

        service.softDeleteOption(
                100L,
                PropertyType.ROOM,
                "BED",
                "매물 옵션 제거",
                actorContext
        );

        assertThat(propertyOption.isDeleted()).isTrue();
        assertThat(propertyOption.getDeletedAt()).isNotNull();
        assertThat(propertyOption.getDeleteReason()).isEqualTo("매물 옵션 제거");
        verify(optionRepository).save(propertyOption);
        verify(optionRepository, never()).delete(propertyOption);
    }

    @Test
    void softDeleteOption_noActivePropertyOption_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", false);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        when(queryRepository.findActiveOptionCode("BED"))
                .thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(100L, 20L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.softDeleteOption(
                100L,
                PropertyType.ROOM,
                "BED",
                "매물 옵션 제거",
                actorContext
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
                );

        verify(optionRepository, never()).save(any(PropertyOption.class));
        verify(optionRepository, never()).delete(any(PropertyOption.class));
    }

    @Test
    void softDeleteOption_multipleActivePropertyOptions_rejects() {
        PropertyOptionCode bed = optionCode(20L, "BED", false);
        PropertyTypeOption bedMapping = typeOption(20L, 1);
        PropertyOption firstOption =
                new PropertyOption(100L, 20L, "true", 1, actorContext);
        PropertyOption secondOption =
                new PropertyOption(100L, 20L, "false", 2, actorContext);
        when(queryRepository.findActiveOptionCode("BED"))
                .thenReturn(Optional.of(bed));
        when(queryRepository.findActiveTypeOptions(PropertyType.ROOM))
                .thenReturn(List.of(bedMapping));
        when(queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(100L, 20L))
                .thenReturn(List.of(firstOption, secondOption));

        assertThatThrownBy(() -> service.softDeleteOption(
                100L,
                PropertyType.ROOM,
                "BED",
                "매물 옵션 제거",
                actorContext
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCustomResponseCode())
                                .isEqualTo(CustomResponseCode.DUPLICATED_RESOURCE)
                );

        assertThat(firstOption.isDeleted()).isFalse();
        assertThat(secondOption.isDeleted()).isFalse();
        verify(optionRepository, never()).save(any(PropertyOption.class));
        verify(optionRepository, never()).delete(any(PropertyOption.class));
    }

    private PropertyOptionCode optionCode(
            Long optionCodeId,
            String optionCode,
            boolean registrationEnabled
    ) {
        PropertyOptionCode entity = mock(PropertyOptionCode.class);
        when(entity.getOptionCodeId()).thenReturn(optionCodeId);
        when(entity.getOptionCode()).thenReturn(optionCode);
        when(entity.isRegistrationEnabled()).thenReturn(registrationEnabled);
        return entity;
    }

    private PropertyTypeOption typeOption(Long optionCodeId, int displayOrder) {
        PropertyTypeOption entity = mock(PropertyTypeOption.class);
        when(entity.getOptionCodeId()).thenReturn(optionCodeId);
        when(entity.getDisplayOrder()).thenReturn(displayOrder);
        return entity;
    }
}
