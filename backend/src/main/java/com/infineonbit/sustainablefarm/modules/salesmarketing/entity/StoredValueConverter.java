package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.AttributeConverter;

/** Stores a {@link StoredValue} enum by its lower-case value. */
public abstract class StoredValueConverter<E extends Enum<E> & StoredValue> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected StoredValueConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return StoredValue.find(type, dbData)
                .orElseThrow(() -> new IllegalStateException("Unknown " + type.getSimpleName() + " value: " + dbData));
    }
}
