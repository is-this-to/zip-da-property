package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;
import com.zipdaproperty.domain.option.entity.PropertyOption;
import com.zipdaproperty.domain.option.entity.PropertyOptionHistory;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyOptionHistoryRepository;
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
import java.util.ArrayList;
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

    private static final String OPTION_REMOVED_FROM_REQUEST_REASON =
            "OPTION_REMOVED_FROM_REQUEST";

    private final PropertyOptionRepository propertyOptionRepository;
    private final PropertyOptionHistoryRepository propertyOptionHistoryRepository;
    private final PropertyOptionQueryDSLRepository queryRepository;
    private final OptionValueValidator optionValueValidator;

    @Transactional
    public void createOption(
            Long propertyId,
            Long propertyRevisionId,
            PropertyType propertyType,
            String optionCode,
            String optionValue,
            String changedFields,
            ActorContext actorContext
    ) {
        createOptions(
                propertyId,
                propertyRevisionId,
                propertyType,
                List.of(new PropertyOptionCreateCommand(optionCode, optionValue)),
                changedFields,
                actorContext
        );
    }

    @Transactional
    public void createOptions(
            Long propertyId,
            Long propertyRevisionId,
            PropertyType propertyType,
            List<PropertyOptionCreateCommand> commands,
            String changedFields,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        validateHistoryContext(propertyRevisionId, changedFields);
        Objects.requireNonNull(propertyType, "매물 유형은 필수입니다.");
        Objects.requireNonNull(commands, "옵션 요청 목록은 필수입니다.");
        Objects.requireNonNull(actorContext, "옵션 생성에는 ActorContext가 필요합니다.");

        ValidatedOptionRequest validatedRequest = validateRequestedOptions(
                propertyType,
                commands
        );

        if (commands.isEmpty()) {
            return;
        }

        Set<Long> activeOptionCodeIds =
                queryRepository.findActiveOptionsByPropertyId(propertyId)
                        .stream()
                        .map(PropertyOption::getOptionCodeId)
                        .collect(Collectors.toSet());

        List<PropertyOption> propertyOptions = commands.stream()
                .map(command -> createPropertyOption(
                        propertyId,
                        command,
                        validatedRequest.optionCodeByCode(),
                        validatedRequest.typeOptionByCodeId(),
                        activeOptionCodeIds,
                        actorContext
                ))
                .toList();

        List<PropertyOption> savedOptions = propertyOptionRepository.saveAll(propertyOptions);
        Instant occurredAt = Instant.now();
        List<PropertyOptionHistory> histories = savedOptions.stream()
                .map(propertyOption -> PropertyOptionHistory.create(
                        propertyRevisionId,
                        propertyOption,
                        changedFields,
                        occurredAt,
                        actorContext
                ))
                .toList();
        propertyOptionHistoryRepository.saveAll(histories);
    }

    @Transactional
    public void synchronizeOptions(
            Long propertyId,
            Long propertyRevisionId,
            PropertyType propertyType,
            List<PropertyOptionCreateCommand> commands,
            String changedFields,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        validateHistoryContext(propertyRevisionId, changedFields);
        Objects.requireNonNull(propertyType, "매물 유형은 필수입니다.");
        Objects.requireNonNull(commands, "옵션 요청 목록은 필수입니다.");
        Objects.requireNonNull(actorContext, "옵션 동기화에는 ActorContext가 필요합니다.");

        ValidatedOptionRequest validatedRequest = validateRequestedOptions(
                propertyType,
                commands
        );

        List<PropertyOption> activeOptions =
                queryRepository.findActiveOptionsByPropertyId(propertyId);
        Map<Long, PropertyOption> activeOptionByCodeId = activeOptions.stream()
                .collect(Collectors.toMap(
                        PropertyOption::getOptionCodeId,
                        Function.identity(),
                        (first, ignored) -> {
                            throw duplicatedOptionCodeId(first.getOptionCodeId());
                        }
                ));

        Instant occurredAt = Instant.now();
        List<PropertyOptionHistory> histories = new ArrayList<>();
        List<PropertyOption> changedOptions = new ArrayList<>();

        for (PropertyOptionCreateCommand command : commands) {
            PropertyOptionCode optionCode =
                    validatedRequest.optionCodeByCode().get(command.optionCode());
            PropertyOption currentOption =
                    activeOptionByCodeId.remove(optionCode.getOptionCodeId());
            int targetDisplayOrder = validatedRequest.typeOptionByCodeId()
                    .get(optionCode.getOptionCodeId())
                    .getDisplayOrder();

            if (currentOption == null) {
                PropertyOption createdOption = new PropertyOption(
                        propertyId,
                        optionCode.getOptionCodeId(),
                        command.optionValue(),
                        targetDisplayOrder,
                        actorContext
                );
                PropertyOption savedOption = propertyOptionRepository.save(createdOption);
                histories.add(PropertyOptionHistory.create(
                        propertyRevisionId,
                        savedOption,
                        changedFields,
                        occurredAt,
                        actorContext
                ));
                continue;
            }

            if (currentOption.getOptionValue().equals(command.optionValue())
                    && currentOption.getDisplayOrder() == targetDisplayOrder) {
                continue;
            }

            PropertyOptionHistory.Snapshot before =
                    PropertyOptionHistory.Snapshot.from(currentOption);
            currentOption.changeValueAndDisplayOrder(
                    command.optionValue(),
                    targetDisplayOrder,
                    actorContext
            );
            changedOptions.add(currentOption);
            histories.add(PropertyOptionHistory.update(
                    propertyRevisionId,
                    currentOption,
                    changedFields,
                    before,
                    occurredAt,
                    actorContext
            ));
        }

        for (PropertyOption removedOption : activeOptionByCodeId.values()) {
            PropertyOptionHistory.Snapshot before =
                    PropertyOptionHistory.Snapshot.from(removedOption);
            removedOption.softDelete(
                    actorContext,
                    occurredAt,
                    OPTION_REMOVED_FROM_REQUEST_REASON
            );
            changedOptions.add(removedOption);
            histories.add(PropertyOptionHistory.softDelete(
                    propertyRevisionId,
                    removedOption,
                    changedFields,
                    before,
                    occurredAt,
                    actorContext
            ));
        }

        if (!changedOptions.isEmpty()) {
            propertyOptionRepository.saveAll(changedOptions);
        }
        if (!histories.isEmpty()) {
            propertyOptionHistoryRepository.saveAll(histories);
        }
    }

    @Transactional
    public void changeOptionValue(
            Long propertyId,
            Long propertyRevisionId,
            PropertyType propertyType,
            String optionCode,
            String optionValue,
            String changedFields,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        validateHistoryContext(propertyRevisionId, changedFields);
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

        if (propertyOption.getOptionValue().equals(optionValue)) {
            return;
        }

        PropertyOptionHistory.Snapshot before =
                PropertyOptionHistory.Snapshot.from(propertyOption);
        Instant occurredAt = Instant.now();
        propertyOption.changeValue(optionValue, actorContext);
        PropertyOption savedOption = propertyOptionRepository.save(propertyOption);
        propertyOptionHistoryRepository.save(PropertyOptionHistory.update(
                propertyRevisionId,
                savedOption,
                changedFields,
                before,
                occurredAt,
                actorContext
        ));
    }

    @Transactional
    public void softDeleteOption(
            Long propertyId,
            Long propertyRevisionId,
            PropertyType propertyType,
            String optionCode,
            String deleteReason,
            String changedFields,
            ActorContext actorContext
    ) {
        Objects.requireNonNull(propertyId, "매물 ID는 필수입니다.");
        validateHistoryContext(propertyRevisionId, changedFields);
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

        PropertyOptionHistory.Snapshot before =
                PropertyOptionHistory.Snapshot.from(propertyOption);
        Instant occurredAt = Instant.now();
        propertyOption.softDelete(actorContext, occurredAt, deleteReason);
        PropertyOption savedOption = propertyOptionRepository.save(propertyOption);
        propertyOptionHistoryRepository.save(PropertyOptionHistory.softDelete(
                propertyRevisionId,
                savedOption,
                changedFields,
                before,
                occurredAt,
                actorContext
        ));
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

    private ValidatedOptionRequest validateRequestedOptions(
            PropertyType propertyType,
            List<PropertyOptionCreateCommand> commands
    ) {
        LinkedHashSet<String> requestedCodes = validateCommands(commands);
        List<PropertyTypeOption> activeTypeOptions =
                queryRepository.findActiveTypeOptions(propertyType);

        Map<String, PropertyOptionCode> optionCodeByCode =
                queryRepository.findActiveOptionCodesByCodes(List.copyOf(requestedCodes))
                        .stream()
                        .collect(Collectors.toMap(
                                PropertyOptionCode::getOptionCode,
                                Function.identity()
                        ));
        Map<Long, PropertyTypeOption> typeOptionByCodeId = activeTypeOptions.stream()
                .collect(Collectors.toMap(
                        PropertyTypeOption::getOptionCodeId,
                        Function.identity(),
                        (first, ignored) -> {
                            throw duplicatedActiveTypeOption(
                                    propertyType,
                                    first.getOptionCodeId()
                            );
                        }
                ));

        for (PropertyOptionCreateCommand command : commands) {
            validateRequestedOption(
                    propertyType,
                    command.optionCode(),
                    optionCodeByCode,
                    typeOptionByCodeId
            );
        }

        validateRequiredOptions(requestedCodes, activeTypeOptions);
        return new ValidatedOptionRequest(optionCodeByCode, typeOptionByCodeId);
    }

    private void validateRequestedOption(
            PropertyType propertyType,
            String requestedOptionCode,
            Map<String, PropertyOptionCode> optionCodeByCode,
            Map<Long, PropertyTypeOption> typeOptionByCodeId
    ) {
        PropertyOptionCode optionCode = optionCodeByCode.get(requestedOptionCode);
        if (optionCode == null) {
            throw new OptionCodeNotFoundException(
                    "존재하지 않거나 비활성화된 옵션 코드입니다: "
                            + requestedOptionCode
            );
        }
        if (!typeOptionByCodeId.containsKey(optionCode.getOptionCodeId())) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "해당 매물 유형에서 사용할 수 없는 옵션입니다: "
                            + requestedOptionCode
                            + " ("
                            + propertyType
                            + ")"
            );
        }
        if (!optionCode.isRegistrationEnabled()) {
            throw new OptionNotAllowedForPropertyTypeException(
                    "등록할 수 없는 옵션입니다: " + requestedOptionCode
            );
        }
    }

    private PropertyOption createPropertyOption(
            Long propertyId,
            PropertyOptionCreateCommand command,
            Map<String, PropertyOptionCode> optionCodeByCode,
            Map<Long, PropertyTypeOption> typeOptionByCodeId,
            Set<Long> activeOptionCodeIds,
            ActorContext actorContext
    ) {
        PropertyOptionCode optionCode = optionCodeByCode.get(command.optionCode());
        PropertyTypeOption typeOption = typeOptionByCodeId.get(optionCode.getOptionCodeId());

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

    private void validateRequiredOptions(
            Set<String> requestedCodes,
            List<PropertyTypeOption> activeTypeOptions
    ) {
        List<Long> requiredOptionCodeIds = activeTypeOptions.stream()
                .filter(PropertyTypeOption::isRequired)
                .map(PropertyTypeOption::getOptionCodeId)
                .distinct()
                .toList();

        if (requiredOptionCodeIds.isEmpty()) {
            return;
        }

        List<PropertyOptionCode> activeRequiredOptionCodes =
                queryRepository.findActiveOptionCodesByIds(requiredOptionCodeIds);
        Set<Long> activeRequiredOptionCodeIds = activeRequiredOptionCodes.stream()
                .map(PropertyOptionCode::getOptionCodeId)
                .collect(Collectors.toSet());
        List<Long> invalidRequiredOptionCodeIds = requiredOptionCodeIds.stream()
                .filter(optionCodeId -> !activeRequiredOptionCodeIds.contains(optionCodeId))
                .toList();

        if (!invalidRequiredOptionCodeIds.isEmpty()) {
            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "필수 옵션 정책이 비활성 또는 삭제된 옵션 코드를 참조합니다: "
                            + invalidRequiredOptionCodeIds
            );
        }

        List<String> missingRequiredOptionCodes = activeRequiredOptionCodes
                        .stream()
                        .map(PropertyOptionCode::getOptionCode)
                        .filter(optionCode -> !requestedCodes.contains(optionCode))
                        .toList();

        if (!missingRequiredOptionCodes.isEmpty()) {
            throw new OptionValueRequiredException(
                    "필수 옵션이 누락되었습니다: "
                            + String.join(", ", missingRequiredOptionCodes)
            );
        }
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

    private void validateHistoryContext(
            Long propertyRevisionId,
            String changedFields
    ) {
        Objects.requireNonNull(propertyRevisionId, "매물 리비전 ID는 필수입니다.");
        if (changedFields == null || changedFields.isBlank()) {
            throw new IllegalArgumentException("변경 필드는 필수입니다.");
        }
        if (changedFields.length() > 500) {
            throw new IllegalArgumentException("변경 필드는 500자를 초과할 수 없습니다.");
        }
    }

    private BusinessException duplicatedOption(String optionCode) {
        return new BusinessException(
                CustomResponseCode.DUPLICATED_RESOURCE,
                "이미 등록된 옵션입니다: " + optionCode
        );
    }

    private BusinessException duplicatedOptionCodeId(Long optionCodeId) {
        return new BusinessException(
                CustomResponseCode.DUPLICATED_RESOURCE,
                "동일한 활성 옵션이 여러 건 존재합니다: optionCodeId = " + optionCodeId
        );
    }

    private BusinessException duplicatedActiveTypeOption(
            PropertyType propertyType,
            Long optionCodeId
    ) {
        return new BusinessException(
                CustomResponseCode.SYSTEM_ERROR,
                "활성 유형별 옵션 정책이 중복되었습니다: propertyType = "
                        + propertyType
                        + ", optionCodeId = "
                        + optionCodeId
        );
    }

    private record ValidatedOptionRequest(
            Map<String, PropertyOptionCode> optionCodeByCode,
            Map<Long, PropertyTypeOption> typeOptionByCodeId
    ) {
    }
}
