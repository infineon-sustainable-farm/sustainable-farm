package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;

import java.util.Optional;

/**
 * An enum of Sales and Marketing whose constants keep the lower-case value the API and the
 * database have always used ({@code in_transit}, {@code direct_export}): the value is stored by
 * {@link StoredValueConverter}, sent in the JSON and matched by the check constraint of its column.
 *
 * <p>Hibernate writes that check constraint itself from the values of the enum, inline on the
 * column ({@code status varchar(20) check (status in (...))}); PostgreSQL names it
 * {@code <table>_<column>_check}, the names of the former migration ({@code customers_status_check},
 * {@code shipments_status_check}...). An explicit {@code @Check} of the same name would collide with it.
 */
public interface StoredValue {

    /** Message of a value outside the enum, as the check constraint of the column refused it. */
    String REFUSED_VALUE = "The request conflicts with the current state of the data. Please refresh and try again.";

    /** The value stored and sent, for example {@code in_transit}. */
    String value();

    /** The constant of a value, or empty for {@code null} or an unknown value. */
    static <E extends Enum<E> & StoredValue> Optional<E> find(Class<E> type, String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.value().equals(value)) {
                return Optional.of(constant);
            }
        }
        return Optional.empty();
    }

    /**
     * The constant of a value sent by a client, {@code null} for {@code null}.
     *
     * @throws ConflictException (409) for an unknown value, the answer the check constraint gave
     */
    static <E extends Enum<E> & StoredValue> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        return find(type, value).orElseThrow(() -> new ConflictException(REFUSED_VALUE));
    }
}
