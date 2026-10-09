package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

import java.util.List;

/** Unknown growth phase code. Answered as a 404 ApiError by the core exception handler. */
public class GrowthPhaseNotFoundException extends ResourceNotFoundException {
    public GrowthPhaseNotFoundException(String code, List<String> codes) {
        super("No growth phase with code " + code + ". The codes are "
                + String.join(", ", codes.subList(0, codes.size() - 1)) + " and " + codes.get(codes.size() - 1));
    }
}
