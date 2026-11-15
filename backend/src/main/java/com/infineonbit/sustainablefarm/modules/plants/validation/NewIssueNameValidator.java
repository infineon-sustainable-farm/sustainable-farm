package com.infineonbit.sustainablefarm.modules.plants.validation;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link NewIssueName}. It reports at most one violation: a blank
 * name, a name that is too long, a name that gives no code, or a name that
 * gives {@code OTHER}, in that order. The code is computed as the service
 * computes it, by {@link HealthIssueReference#codeOf(String)}.
 */
public class NewIssueNameValidator implements ConstraintValidator<NewIssueName, String> {

    /** Longest name, once trimmed: its code then fits the length of a code. */
    static final int MAX_LENGTH = HealthIssueReference.CODE_MAX_LENGTH;

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null) {
            return true;
        }
        String trimmed = name.trim();
        String code = HealthIssueReference.codeOf(trimmed);

        String message = null;
        if (trimmed.isEmpty()) {
            message = "name is required";
        } else if (trimmed.length() > MAX_LENGTH) {
            message = "name must be at most " + MAX_LENGTH + " characters";
        } else if (code.isEmpty()) {
            message = "name must contain a letter from A to Z or a digit";
        } else if (code.equals(HealthIssueReference.OTHER_CODE)) {
            message = "name must not give the code OTHER, which is reserved for problems outside the catalogue";
        }
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
