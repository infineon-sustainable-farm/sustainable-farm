package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks the measured value of a maturity observation: it is left out unless
 * the method measures one ({@code BRIX} or {@code DRY_MATTER}), and it is above
 * 0 and at most {@value MeasuredValueMatchesMethodValidator#MAX_VALUE}, with
 * one decimal at most. A missing value is always accepted.
 *
 * <p>Put on {@code MaturityObservationRequest}. The violation is reported on
 * {@code measuredValue}, so the 400 lists it under that field.
 *
 * <p>The range and the decimals are checked here, not by constraints on the
 * value itself: they could fail on the same value as the method rule, and the
 * reported message would then depend on the order of the violations, since
 * only one message per field is kept.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MeasuredValueMatchesMethodValidator.class)
public @interface MeasuredValueMatchesMethod {

    /** Unused: each violation carries its own message. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
