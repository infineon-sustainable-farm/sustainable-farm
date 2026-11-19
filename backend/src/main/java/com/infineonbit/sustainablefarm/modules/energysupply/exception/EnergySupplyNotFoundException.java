package com.infineonbit.sustainablefarm.modules.energysupply.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class EnergySupplyNotFoundException extends ResourceNotFoundException {

    public EnergySupplyNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }
}
