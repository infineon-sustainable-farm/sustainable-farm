package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SalesMarketingRequestMapper {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules();

    public <T> T toEntity(Object request, Class<T> entityType) {
        return OBJECT_MAPPER.convertValue(request, entityType);
    }
}
