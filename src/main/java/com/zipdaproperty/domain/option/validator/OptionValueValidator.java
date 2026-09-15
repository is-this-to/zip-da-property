package com.zipdaproperty.domain.option.validator;

import org.springframework.stereotype.Component;

@Component
public class OptionValueValidator {

  private static final String TRUE_VALUE = "true";
  private static final String FALSE_VALUE = "false";

  public boolean isValid(String optionValue) {
    return TRUE_VALUE.equals(optionValue)
        || FALSE_VALUE.equals(optionValue);
  }
}
