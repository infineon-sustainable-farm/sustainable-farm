package com.infineonbit.sustainablefarm.modules.plants.exception;

public class GrowthCalendarNotFoundException extends RuntimeException {
    public GrowthCalendarNotFoundException(Long id) {
        super("Growth calendar entry with ID " + id + " not found");
    }
}
