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
import com.zipdaproperty.global.error.custom.business.OptionValueRequiredException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyOptionCommandService {

    private final PropertyOptionRepository propertyOptionRepository;
    private final PropertyOptionQueryDSLRepository queryRepository;
    private final OptionValueValidator optionValueValidator;

    @Transactional
    public void createOption(
            Long propertyId,
            PropertyType propertyType,
            String optionCode,
            String optionValue,
            ActorContext actorContext
    ) {
        createOptions(
                propertyId,
                propertyType,
                List.of(new PropertyOptionCreateCommand(optionCode, optionValue)),
                actorContext
        );
    }

    @Transactional
    public void createOptions(
            Long propertyId,
            PropertyType propertyType,
            List<PropertyOptionCreateCommand> commands,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        Objects.requireNonNull(propertyType, "매물 유형은 필수입니다.");
        Objects.requireNonNull(commands, "옵션 요청 목록은 필수입니다.");
        Objects.requireNonNull(actorContext, "옵션 생성에는 ActorContext가 필요합니다.");

        if (commands.isEmpty()) {
            return;
        }

        LinkedHashSet<String> requestedCodes = validateCommands(commands);

        Map<String, PropertyOptionCode> optionCodeByCode =
                queryRepository.findActiveOptionCodesByCodes(List.copyOf(requestedCodes))
                        .stream()
                        .collect(Collectors.toMap(
                                PropertyOptionCode::getOptionCode,
                                Function.identity()
                        ));

        Map<Long, PropertyTypeOption> typeOptionByCodeId =
                queryRepository.findActiveTypeOptions(propertyType)
                        .stream()
                        .collect(Collectors.toMap(
                                PropertyTypeOption::getOptionCodeId,
                                Function.identity(),
                                (first, ignored) -> first
                        ));

        Set<Long> activeOptionCodeIds =
                queryRepository.findActiveOptionsByPropertyId(propertyId)
                        .stream()
                        .map(PropertyOption::getOptionCodeId)
                        .collect(Collectors.toSet());

        List<PropertyOption> propertyOptions = commands.stream()
                .map(command -> createPropertyOption(
                        propertyId,
                        propertyType,
                        command,
                        optionCodeByCode,
                        typeOptionByCodeId,
                        activeOptionCodeIds,
                        actorContext
                ))
                .toList();

        propertyOptionRepository.saveAll(propertyOptions);
    }

    @Transactional
    public void changeOptionValue(
            Long propertyId,
            PropertyType propertyType,
            String optionCode,
            String optionValue,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        Objects.requireNonNull(propertyType, "매물 유형은 필수입니다.");
        Objects.requireNonNull(actorContext, "옵션 수정에는 ActorContext가 필요합니다.");

        validateOptionValue(optionCode, optionValue);

        PropertyOptionCode writableOptionCode = findWritableOptionCode(
                propertyType,
                optionCode
        );
        PropertyOption propertyOption = findActivePropertyOption(
                propertyId,
                writableOptionCode.getOptionCodeId(),
                optionCode
        );

        propertyOption.changeValue(optionValue, actorContext);
        propertyOptionRepository.save(propertyOption);
    }

    @Transactional
    public void softDeleteOption(
            Long propertyId,
            PropertyType propertyType,
            String optionCode,
            String deleteReason,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        Objects.requireNonNull(propertyType, "매물 유형은 필수입니다.");
        Objects.requireNonNull(actorContext, "옵션 삭제에는 ActorContext가 필요합니다.");

        PropertyOptionCode deletableOptionCode = findActiveAllowedOptionCode(
                propertyType,
                optionCode
        );
        PropertyOption propertyOption = findActivePropertyOption(
                propertyId,
                deletableOptionCode.getOptionCodeId(),
                optionCode
        );

        propertyOption.softDelete(actorContext, Instant.now(), deleteReason);
        propertyOptionRepository.save(propertyOption);
    }

    private LinkedHashSet<String> validateCommands(
            List<PropertyOptionCreateCommand> commands
    ) {
        LinkedHashSet<String> requestedCodes = new LinkedHashSet<>();

        for (PropertyOptionCreateCommand command : commands) {
            if (command == null || command.optionCode() == null || command.optionCode().isBlank()) {
                throw new OptionCodeNotFoundException(
                        "옵션 코드는 필수입니다."
                );
            }

            if (!requestedCodes.add(command.optionCode())) {
                throw duplicatedOption(command.optionCode());
            }

            validateOptionValue(command.optionCode(), command.optionValue());
        }

        return requestedCodes;
    }

    private PropertyOption createPropertyOption(
            Long propertyId,
            PropertyType propertyType,
            PropertyOptionCreateCommand command,
            Map<String, PropertyOptionCode> optionCodeByCode,
            Map<Long, PropertyTypeOption> typeOptionByCodeId,
            Set<Long> activeOptionCodeIds,
            ActorContext actorContext
    ) {
        PropertyOptionCode optionCode = optionCodeByCode.get(command.optionCode());

        if (optionCode == null) {
            throw new OptionCodeNotFoundException(
                    "존재하지 않거나 비활성화된 옵션 코드입니다: "
                            + command.optionCode()
            );
        }

        PropertyTypeOption typeOption = typeOptionByCodeId.get(optionCode.getOptionCodeId());

        if (typeOption == null) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "해당 매물 유형에서 사용할 수 없는 옵션입니다: "
                            + command.optionCode()
                            + " ("
                            + propertyType
                            + ")"
            );
        }

        if (!optionCode.isRegistrationEnabled()) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "등록할 수 없는 옵션입니다: " + command.optionCode()
            );
        }

        if (activeOptionCodeIds.contains(optionCode.getOptionCodeId())) {
            throw duplicatedOption(command.optionCode());
        }

        return new PropertyOption(
                propertyId,
                optionCode.getOptionCodeId(),
                command.optionValue(),
                typeOption.getDisplayOrder(),
                actorContext
        );
    }

    private PropertyOptionCode findWritableOptionCode(
            PropertyType propertyType,
            String optionCode
    ) {
        PropertyOptionCode propertyOptionCode = findActiveAllowedOptionCode(
                propertyType,
                optionCode
        );

        if (!propertyOptionCode.isRegistrationEnabled()) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "등록하거나 수정할 수 없는 옵션입니다: " + optionCode
            );
        }

        return propertyOptionCode;
    }

    private PropertyOptionCode findActiveAllowedOptionCode(
            PropertyType propertyType,
            String optionCode
    ) {
        if (optionCode == null || optionCode.isBlank()) {
            throw new OptionCodeNotFoundException(
                    "옵션 코드는 필수입니다."
            );
        }

        PropertyOptionCode propertyOptionCode =
                queryRepository.findActiveOptionCode(optionCode)
                        .orElseThrow(() ->
                                new OptionCodeNotFoundException(
                                        "존재하지 않거나 비활성화된 옵션 코드입니다: "
                                                + optionCode
                                )
                        );

        boolean allowedForPropertyType =
                queryRepository.findActiveTypeOptions(propertyType)
                        .stream()
                        .anyMatch(typeOption ->
                                typeOption.getOptionCodeId()
                                        .equals(propertyOptionCode.getOptionCodeId())
                        );

        if (!allowedForPropertyType) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "해당 매물 유형에서 사용할 수 없는 옵션입니다: "
                            + optionCode
                            + " ("
                            + propertyType
                            + ")"
            );
        }

        return propertyOptionCode;
    }

    private PropertyOption findActivePropertyOption(
            Long propertyId,
            Long optionCodeId,
            String optionCode
    ) {
        List<PropertyOption> activeOptions =
                queryRepository.findActiveOptionsByPropertyIdAndOptionCodeId(
                        propertyId,
                        optionCodeId
                );

        if (activeOptions.isEmpty()) {
            throw new BusinessException(
                    CustomResponseCode.NOT_FOUND_RESOURCE,
                    "수정하거나 삭제할 활성 옵션이 없습니다: " + optionCode
            );
        }

        if (activeOptions.size() > 1) {
            throw new BusinessException(
                    CustomResponseCode.DUPLICATED_RESOURCE,
                    "동일한 활성 옵션이 여러 건 존재합니다: " + optionCode
            );
        }

        return activeOptions.getFirst();
    }

    private void validateOptionValue(
            String optionCode,
            String optionValue
    ) {
        if (optionValue == null || optionValue.isBlank()) {
            throw new OptionValueRequiredException(
                    "옵션 값은 필수입니다: " + optionCode
            );
        }

        if (!optionValueValidator.isValid(optionValue)) {
            throw new OptionValueInvalidException(
                    "옵션 값은 true 또는 false만 사용할 수 있습니다: "
                            + optionCode
            );
        }
    }

    private BusinessException duplicatedOption(String optionCode) {
        return new BusinessException(
                CustomResponseCode.DUPLICATED_RESOURCE,
                "이미 등록된 옵션입니다: " + optionCode
        );
    }
}
