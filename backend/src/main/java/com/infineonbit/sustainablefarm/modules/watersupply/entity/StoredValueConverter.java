package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.AttributeConverter;

/** Maps a {@link StoredValue} enum to its lower-case column value and back. */
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
        return StoredValue.parse(type, dbData, type.getSimpleName());
    }
}
