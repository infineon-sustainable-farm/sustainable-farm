package com.infineonbit.sustainablefarm.modules.machinery.exception;

public class UsageLogNotFoundException extends RuntimeException {
    public UsageLogNotFoundException(Long id) {
        super("Usage log not found with id: " + id);
    }
}
