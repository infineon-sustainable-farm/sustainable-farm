package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks the free label of a request that names an issue of the catalogue: the
 * label is required when the code is {@code OTHER}, refused with any other
 * code, and at most {@value OtherLabelMatchesIssueValidator#MAX_LENGTH}
 * characters.
 *
 * <p>Put on a record; {@link #codeField()} and {@link #labelField()} name its
 * two components, and the messages use those names. The violation is reported
 * on the label, so the 400 lists it under that field.
 *
 * <p>The length is checked here, not by a {@code @Size} on the label: the two
 * could fail on the same value, and the reported message would then depend on
 * the order of the violations, since only one message per field is kept.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OtherLabelMatchesIssueValidator.class)
public @interface OtherLabelMatchesIssue {

    /** Name of the record component holding the issue code, for example {@code "issueCode"}. */
    String codeField();

    /** Name of the record component holding the free label, for example {@code "otherLabel"}. */
    String labelField();

    /** Unused: each violation carries its own message, built from the component names. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
