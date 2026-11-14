package com.infineonbit.sustainablefarm.modules.energysupply.exception;

public class EnergySupplyNotFoundException extends RuntimeException {

    public EnergySupplyNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }
}
