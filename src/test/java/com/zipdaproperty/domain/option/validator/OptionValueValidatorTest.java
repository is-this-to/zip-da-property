package com.zipdaproperty.domain.option.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptionValueValidatorTest {

  private final OptionValueValidator validator = new OptionValueValidator();

  @Test
  void isValid_trueFalse_accepts() {
    assertThat(validator.isValid("true")).isTrue();
    assertThat(validator.isValid("false")).isTrue();
  }

  @Test
  void isValid_missingValue_rejects() {
    assertThat(validator.isValid(null)).isFalse();
    assertThat(validator.isValid("")).isFalse();
    assertThat(validator.isValid(" ")).isFalse();
  }

  @Test
  void isValid_otherValues_rejects() {
    assertThat(validator.isValid("TRUE")).isFalse();
    assertThat(validator.isValid("FALSE")).isFalse();
    assertThat(validator.isValid("True")).isFalse();
    assertThat(validator.isValid("False")).isFalse();

    assertThat(validator.isValid("1")).isFalse();
    assertThat(validator.isValid("0")).isFalse();

    assertThat(validator.isValid("있음")).isFalse();
    assertThat(validator.isValid("없음")).isFalse();

    assertThat(validator.isValid("true ")).isFalse();
    assertThat(validator.isValid(" true")).isFalse();
    assertThat(validator.isValid("미확인")).isFalse();
  }

}