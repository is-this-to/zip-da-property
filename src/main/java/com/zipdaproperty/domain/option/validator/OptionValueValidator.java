package com.zipdaproperty.domain.option.validator;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.type.OptionValueType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OptionValueValidator {

    private final JsonMapper jsonMapper;

    public void validate(
            PropertyOptionCode optionCode,
            Boolean booleanValue,
            BigDecimal numberValue,
            String textValue
    ) {
        OptionValueType valueType = optionCode.getValueType();

        switch (valueType) {
            case BOOLEAN ->
                    validateBoolean(booleanValue, numberValue, textValue);

            case NUMBER ->
                    validateNumber(booleanValue, numberValue, textValue);

            case TEXT ->
                    validateText(booleanValue, numberValue, textValue);

            case SINGLE_SELECT ->
                    validateSingleSelect(
                            optionCode,
                            booleanValue,
                            numberValue,
                            textValue
                    );
        }
    }

    private void validateSingleSelect(
            PropertyOptionCode optionCode,
            Boolean booleanValue,
            BigDecimal numberValue,
            String textValue
    ) {
        validateText(
                booleanValue,
                numberValue,
                textValue
        );

        List<String> allowedValues = readAllowedValues(
                optionCode.getAllowedValuesJson()
        );

        if (!allowedValues.contains(textValue)) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH,
                    "허용되지 않은 SINGLE_SELECT 옵션 값입니다."
            );
        }
    }

    private List<String> readAllowedValues(
            String allowedValuesJson
    ) {
        if (allowedValuesJson == null || allowedValuesJson.isBlank()) {
            return List.of();
        }

        try {
            return jsonMapper.readValue(
                    allowedValuesJson,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "옵션 허용값 설정을 읽을 수 없습니다."
            );
        }
    }

    private void validateBoolean(
            Boolean booleanValue,
            BigDecimal numberValue,
            String textValue
    ) {
        if (booleanValue == null) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_REQUIRED,
                    "BOOLEAN 옵션 값이 필요합니다."
            );
        }

        if (numberValue != null || textValue != null) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH,
                    "BOOLEAN 옵션에는 booleanValue만 사용할 수 있습니다."
            );
        }
    }

    private void validateNumber(
            Boolean booleanValue,
            BigDecimal numberValue,
            String textValue
    ) {
        if (numberValue == null) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_REQUIRED,
                    "NUMBER 옵션 값이 필요합니다."
            );
        }

        if (booleanValue != null || textValue != null) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH,
                    "NUMBER 옵션에는 numberValue만 사용할 수 있습니다."
            );
        }
    }

    private void validateText(
            Boolean booleanValue,
            BigDecimal numberValue,
            String textValue
    ) {
        if (textValue == null || textValue.isBlank()) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_REQUIRED,
                    "TEXT 또는 SINGLE_SELECT 옵션 값이 필요합니다."
            );
        }

        if (booleanValue != null || numberValue != null) {
            throw new BusinessException(
                    CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH,
                    "TEXT 또는 SINGLE_SELECT 옵션에는 textValue만 사용할 수 있습니다."
            );
        }
    }
}