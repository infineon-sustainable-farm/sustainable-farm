package com.infineonbit.sustainablefarm.modules.machinery.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

public class UsageLogNotFoundException extends ResourceNotFoundException {
    public UsageLogNotFoundException(Long id) {
        super("Usage log not found with id: " + id);
    }
}
