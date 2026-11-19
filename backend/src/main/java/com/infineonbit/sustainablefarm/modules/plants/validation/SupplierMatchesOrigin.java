package com.infineonbit.sustainablefarm.modules.plants.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks the supplier fields of a nursery batch against its origin: the
 * supplier is required when the plants were purchased and refused otherwise;
 * the supplier lot number is optional, and refused unless the plants were
 * purchased. Both lengths are checked too.
 *
 * <p>Put on {@link com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest}.
 * Each violation is reported on its own field, so the 400 lists it under
 * {@code supplier} or {@code supplierLotNumber}.
 *
 * <p>The lengths are checked here, not by a {@code @Size}: the two could fail
 * on the same value, and the reported message would then depend on the order
 * of the violations, since only one message per field is kept.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SupplierMatchesOriginValidator.class)
public @interface SupplierMatchesOrigin {

    /** Unused: each violation carries its own message. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
