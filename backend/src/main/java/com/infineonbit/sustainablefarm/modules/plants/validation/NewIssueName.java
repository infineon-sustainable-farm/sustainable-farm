package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks the name of a pest or disease added to the catalogue, which its code
 * is derived from: not blank, at most
 * {@value NewIssueNameValidator#MAX_LENGTH} characters once trimmed, with a
 * letter from A to Z, accented or not, or a digit, and not giving
 * {@code OTHER}, the code that "Other" keeps.
 *
 * <p>One constraint checks the four rules, so that a value breaking two of
 * them, such as 60 spaces, gets one message whatever the order of the
 * violations: only one message per field is kept. A {@code null} name is left
 * to {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = NewIssueNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NewIssueName {

    /** Unused: each violation carries its own message. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
