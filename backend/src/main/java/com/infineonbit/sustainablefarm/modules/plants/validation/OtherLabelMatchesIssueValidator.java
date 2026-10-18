package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

/**
 * Validator of {@link OtherLabelMatchesIssue}. It reports at most one violation,
 * on the label: a missing label with {@code OTHER}, a label with another code,
 * or a label that is too long, in that order.
 *
 * <p>A missing or malformed code is left to the constraints of the code itself.
 * The code is compared without case or surrounding spaces, as the service
 * normalizes it; a blank label counts as no label.
 */
public class OtherLabelMatchesIssueValidator implements ConstraintValidator<OtherLabelMatchesIssue, Record> {

    /** Longest label accepted, in characters. */
    static final int MAX_LENGTH = 60;

    private static final String OTHER_CODE = "OTHER";

    private String codeField;
    private String labelField;

    @Override
    public void initialize(OtherLabelMatchesIssue annotation) {
        codeField = annotation.codeField();
        labelField = annotation.labelField();
    }

    /**
     * Reads a component of the record by name.
     *
     * @throws IllegalStateException if the annotation names a component the record does not have
     */
    private static String component(Record value, String name) {
        RecordComponent component = Arrays.stream(value.getClass().getRecordComponents())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        value.getClass().getSimpleName() + " has no component named " + name));
        try {
            return (String) component.getAccessor().invoke(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read " + name + " of " + value.getClass().getSimpleName(),
                    exception);
        }
    }

    @Override
    public boolean isValid(Record value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String code = component(value, codeField);
        if (code == null) {
            return true;
        }
        String label = component(value, labelField);
        boolean isOther = code.trim().equalsIgnoreCase(OTHER_CODE);
        boolean hasLabel = label != null && !label.isBlank();

        String message = null;
        if (isOther && !hasLabel) {
            message = labelField + " is required when " + codeField + " is OTHER";
        } else if (!isOther && hasLabel) {
            message = labelField + " must be left out unless " + codeField + " is OTHER";
        } else if (hasLabel && label.length() > MAX_LENGTH) {
            message = labelField + " must be at most " + MAX_LENGTH + " characters";
        }
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(labelField)
                .addConstraintViolation();
        return false;
    }
}
