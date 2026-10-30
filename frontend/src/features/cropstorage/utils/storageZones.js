function getCapacityKg(zone) {
    return Number(zone.capacity_kg ?? zone.capacityKg ?? 0);
}

function getZoneKey(zone) {
    const key = zone.id ?? zone.zoneCode ?? zone.code;
    return key == null ? null : String(key);
}

function getBatchZoneKey(batch) {
    const key =
        batch.storageZone?.id ??
        batch.storageZoneId ??
        batch.storage_zone_id ??
        batch.storageZone?.zoneCode ??
        batch.zoneCode;
    return key == null ? null : String(key);
}

export function withZoneUsage(zones, batches) {
    const quantities = new Map();
    for (const batch of batches) {
        const key = getBatchZoneKey(batch);
        if (key === null) continue;
        const quantity = Number(batch.current_quantity_kg ?? batch.currentQuantityKg ?? 0);
        quantities.set(key, (quantities.get(key) ?? 0) + quantity);
    }

    return zones.map((zone) => {
        const capacityKg = getCapacityKg(zone);
        const explicitCapacity = zone.capacity_m3 ?? zone.capacityM3;
        const zoneId = getZoneKey(zone);
        const usedKg = quantities.get(zoneId) ?? 0;
        return {
            ...zone,
            capacityKg,
            capacityM3: explicitCapacity == null ? capacityKg / 700 : Number(explicitCapacity),
            usedKg,
            usedPercent: capacityKg > 0 ? (usedKg / capacityKg) * 100 : 0,
        };
    });
}
