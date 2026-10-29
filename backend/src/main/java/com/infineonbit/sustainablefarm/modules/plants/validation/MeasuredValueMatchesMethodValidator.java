package com.infineonbit.sustainablefarm.modules.plants.validation;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

/**
 * Validator of {@link MeasuredValueMatchesMethod}. It reports at most one
 * violation, on {@code measuredValue}: a value with a method that measures
 * none, then a value out of range or with more than one decimal.
 *
 * <p>A missing or malformed method is left to the constraints of the method
 * itself; the range is still checked. The method is compared without case or
 * surrounding spaces, as the service normalizes it.
 */
public class MeasuredValueMatchesMethodValidator
        implements ConstraintValidator<MeasuredValueMatchesMethod, MaturityObservationRequest> {

    /** Highest value accepted: °Bx and dry matter are both a percentage of the mass. */
    static final double MAX_VALUE = 100.0;

    private static final String FIELD = "measuredValue";

    /** The method of the request, or {@code null} when it is missing or not a method. */
    private static MaturityMethod methodOf(String method) {
        if (method == null) {
            return null;
        }
        String code = method.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(MaturityMethod.values())
                .filter(candidate -> candidate.name().equals(code))
                .findFirst()
                .orElse(null);
    }

    /** Whether a value lies in the range and has one decimal at most. */
    private static boolean isInRange(double value) {
        // The range is tested first: a value outside it, infinite included, never reaches BigDecimal.
        return value > 0 && value <= MAX_VALUE && BigDecimal.valueOf(value).stripTrailingZeros().scale() <= 1;
    }

    @Override
    public boolean isValid(MaturityObservationRequest request, ConstraintValidatorContext context) {
        if (request == null || request.measuredValue() == null) {
            return true;
        }
        MaturityMethod method = methodOf(request.method());
        String message = null;
        if (method != null && !method.measuresAValue()) {
            message = FIELD + " must be left out unless method is BRIX or DRY_MATTER";
        } else if (!isInRange(request.measuredValue())) {
            message = FIELD + " must be above 0 and at most 100, with at most one decimal";
        }
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(FIELD)
                .addConstraintViolation();
        return false;
    }
}
