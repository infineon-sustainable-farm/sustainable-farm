package com.infineonbit.sustainablefarm.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Accepts international phone numbers allowing an optional leading '+',
 * digits, spaces, hyphens, dots and parentheses (max 30 characters).
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9()\\-. ]{5,28}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PHONE_PATTERN.matcher(value.trim()).matches();
    }
}
