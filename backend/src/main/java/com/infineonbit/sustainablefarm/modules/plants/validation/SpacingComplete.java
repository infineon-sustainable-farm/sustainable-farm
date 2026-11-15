package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks that a planting gives both spacings or none: a single spacing would
 * show as no spacing on the variety list.
 *
 * <p>Put on {@link com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest}.
 * The violation is reported on the missing field, so the 400 lists it under
 * {@code rowSpacingM} or {@code treeSpacingM}. The bounds of each spacing are
 * left to its own constraints.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SpacingCompleteValidator.class)
public @interface SpacingComplete {

    /** Unused: the violation carries its own message. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
