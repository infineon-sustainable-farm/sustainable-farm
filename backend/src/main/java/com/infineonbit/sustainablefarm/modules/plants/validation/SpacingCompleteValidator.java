package com.infineonbit.sustainablefarm.modules.plants.validation;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link SpacingComplete}. When only one spacing is given, it
 * reports the other one as required.
 */
public class SpacingCompleteValidator implements ConstraintValidator<SpacingComplete, PlantingRequest> {

    @Override
    public boolean isValid(PlantingRequest value, ConstraintValidatorContext context) {
        if (value == null || (value.rowSpacingM() == null) == (value.treeSpacingM() == null)) {
            return true;
        }
        String missing = value.rowSpacingM() == null ? "rowSpacingM" : "treeSpacingM";
        String given = value.rowSpacingM() == null ? "treeSpacingM" : "rowSpacingM";
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(missing + " is required when " + given + " is given")
                .addPropertyNode(missing)
                .addConstraintViolation();
        return false;
    }
}
