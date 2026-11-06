function toNonNegativeNumber(value) {
    if (value == null || (typeof value === "string" && value.trim() === "")) return null;
    const number = Number(value);
    return Number.isFinite(number) && number >= 0 ? number : null;
}

function getIdentifier(value) {
    if (value == null) return null;
    const key = String(value).trim();
    return key ? key : null;
}

function getZoneKeys(zone) {
    return [
        zone.id,
        zone.zoneCode,
        zone.zone_code,
        zone.code,
    ]
        .map(getIdentifier)
        .filter((key, index, keys) => key !== null && keys.indexOf(key) === index);
}

function getBatchZoneKeys(batch) {
    return [
        batch.storageZone?.id,
        batch.storageZoneId,
        batch.storage_zone_id,
        batch.storageZone?.zoneCode,
        batch.storageZone?.zone_code,
        batch.storage_zone?.id,
        batch.storage_zone?.zoneCode,
        batch.storage_zone?.zone_code,
        batch.zoneCode,
        batch.zone_code,
    ]
        .map(getIdentifier)
        .filter((key, index, keys) => key !== null && keys.indexOf(key) === index);
}

export function withZoneUsage(zones, batches) {
    const quantities = new Map();
    const zoneKeysByIdentifier = new Map();
    const primaryKeys = zones.map((zone) => {
        const keys = getZoneKeys(zone);
        const primaryKey = keys[0] ?? null;
        if (primaryKey !== null) {
            for (const key of keys) zoneKeysByIdentifier.set(key, primaryKey);
        }
        return primaryKey;
    });

    for (const batch of batches) {
        const key = getBatchZoneKeys(batch)
            .map((identifier) => zoneKeysByIdentifier.get(identifier))
            .find((zoneKey) => zoneKey != null);
        if (key == null) continue;
        const quantity = toNonNegativeNumber(
            batch.current_quantity_kg ?? batch.currentQuantityKg,
        );
        if (quantities.has(key) && quantities.get(key) === null) continue;
        quantities.set(
            key,
            quantity === null
                ? null
                : (quantities.get(key) ?? 0) + quantity,
        );
    }

    return zones.map((zone, index) => {
        const capacityKg = toNonNegativeNumber(zone.capacity_kg ?? zone.capacityKg);
        const explicitCapacity = zone.capacity_m3 ?? zone.capacityM3;
        const zoneId = primaryKeys[index];
        const usedKg =
            zoneId === null
                ? null
                : quantities.has(zoneId)
                  ? quantities.get(zoneId)
                  : 0;
        const validCapacity = capacityKg != null && capacityKg > 0;
        return {
            ...zone,
            capacityKg,
            capacityM3:
                explicitCapacity == null
                    ? capacityKg == null
                        ? null
                        : capacityKg / 700
                    : toNonNegativeNumber(explicitCapacity),
            usedKg,
            currentQuantityKg: usedKg,
            usedPercent:
                validCapacity && usedKg != null ? (usedKg / capacityKg) * 100 : null,
        };
    });
}

export function getOverallUsagePercent(zones) {
    if (zones.length === 0) return null;

    let totalCapacity = 0;
    let totalUsed = 0;
    for (const zone of zones) {
        const capacity = toNonNegativeNumber(zone.capacityKg ?? zone.capacity_kg);
        const used = toNonNegativeNumber(zone.usedKg ?? zone.currentQuantityKg);
        if (capacity == null || capacity <= 0 || used == null) return null;
        totalCapacity += capacity;
        totalUsed += used;
    }

    return totalCapacity > 0 ? (totalUsed / totalCapacity) * 100 : null;
}
