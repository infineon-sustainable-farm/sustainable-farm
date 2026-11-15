package com.infineonbit.sustainablefarm.modules.energysupply.entity;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;

public class EnergyComponentIdGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity) {
        String componentId = ((EnergyComponent) entity).getComponentId();
        if (componentId == null || componentId.isBlank()) {
            throw new IdentifierGenerationException("Energy component ID must be provided");
        }
        return componentId;
    }
}
