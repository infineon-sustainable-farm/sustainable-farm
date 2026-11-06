package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import jakarta.validation.constraints.NotNull;

/**
 * The only client-controlled part of a nested Crop Storage relationship.
 */
public record EntityReferenceRequest(@NotNull Long id) {

    public Batch toBatch() {
        if (id == null) {
            return null;
        }
        Batch batch = new Batch();
        batch.setId(id);
        return batch;
    }

    public Sensor toSensor() {
        if (id == null) {
            return null;
        }
        Sensor sensor = new Sensor();
        sensor.setId(id);
        return sensor;
    }

    public StorageZone toStorageZone() {
        if (id == null) {
            return null;
        }
        StorageZone zone = new StorageZone();
        zone.setId(id);
        return zone;
    }

    public Supplier toSupplier() {
        if (id == null) {
            return null;
        }
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }
}
