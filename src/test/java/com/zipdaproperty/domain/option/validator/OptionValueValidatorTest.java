package com.zipdaproperty.domain.option.validator;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class OptionValueValidatorTest {

    private final OptionValueValidator validator = new OptionValueValidator();

    @Test
    void validate_presenceValues_accepts() {
        assertThatCode(() -> validator.validate("있음")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("없음")).doesNotThrowAnyException();
    }

    @Test
    void validate_missingValue_rejects() {
        for (String value : new String[]{null, "", " "}) {
            BusinessException exception = catchThrowableOfType(
                    BusinessException.class, () -> validator.validate(value)
            );
            assertThat(exception).isNotNull();
            assertThat(exception.getCustomResponseCode())
                    .isEqualTo(CustomResponseCode.OPTION_VALUE_REQUIRED);
        }
    }

    @Test
    void validate_otherValues_rejects() {
        for (String value : new String[]{"true", "false", "1", "있음 ", " 있음", "미확인"}) {
            BusinessException exception = catchThrowableOfType(
                    BusinessException.class, () -> validator.validate(value)
            );
            assertThat(exception).isNotNull();
            assertThat(exception.getCustomResponseCode())
                    .isEqualTo(CustomResponseCode.OPTION_VALUE_TYPE_MISMATCH);
        }
    }
}
