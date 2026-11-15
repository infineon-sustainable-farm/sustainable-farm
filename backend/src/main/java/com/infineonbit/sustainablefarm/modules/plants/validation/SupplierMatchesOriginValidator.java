package com.infineonbit.sustainablefarm.modules.plants.validation;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link SupplierMatchesOrigin}. It reports at most one violation
 * per field: for the supplier, a missing one on a purchased batch, one given on
 * a batch grown on the farm, or one that is too long; for the lot number, one
 * given on a batch grown on the farm, or one that is too long.
 *
 * <p>A missing or malformed origin is left to the constraints of the origin
 * itself. The origin is compared without case or surrounding spaces, as the
 * service normalizes it; a blank value counts as none.
 */
public class SupplierMatchesOriginValidator implements ConstraintValidator<SupplierMatchesOrigin, NurseryBatchRequest> {

    /** Longest supplier name accepted, in characters. */
    static final int SUPPLIER_MAX_LENGTH = 255;

    /** Longest supplier lot number accepted, in characters. */
    static final int LOT_NUMBER_MAX_LENGTH = 50;

    private static boolean isGiven(String value) {
        return value != null && !value.isBlank();
    }

    private static void report(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }

    @Override
    public boolean isValid(NurseryBatchRequest value, ConstraintValidatorContext context) {
        if (value == null || value.origin() == null) {
            return true;
        }
        String origin = value.origin().trim();
        boolean purchased = origin.equalsIgnoreCase(NurseryOrigin.PURCHASED.name());
        if (!purchased && !origin.equalsIgnoreCase(NurseryOrigin.IN_HOUSE.name())) {
            return true;
        }

        String supplierMessage = null;
        boolean hasSupplier = isGiven(value.supplier());
        if (purchased && !hasSupplier) {
            supplierMessage = "supplier is required when origin is PURCHASED";
        } else if (!purchased && hasSupplier) {
            supplierMessage = "supplier must be left out unless origin is PURCHASED";
        } else if (hasSupplier && value.supplier().length() > SUPPLIER_MAX_LENGTH) {
            supplierMessage = "supplier must be at most " + SUPPLIER_MAX_LENGTH + " characters";
        }

        String lotNumberMessage = null;
        boolean hasLotNumber = isGiven(value.supplierLotNumber());
        if (!purchased && hasLotNumber) {
            lotNumberMessage = "supplierLotNumber must be left out unless origin is PURCHASED";
        } else if (hasLotNumber && value.supplierLotNumber().length() > LOT_NUMBER_MAX_LENGTH) {
            lotNumberMessage = "supplierLotNumber must be at most " + LOT_NUMBER_MAX_LENGTH + " characters";
        }

        if (supplierMessage == null && lotNumberMessage == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        if (supplierMessage != null) {
            report(context, "supplier", supplierMessage);
        }
        if (lotNumberMessage != null) {
            report(context, "supplierLotNumber", lotNumberMessage);
        }
        return false;
    }
}
