import { apiClient } from "../../shared/api/client";

const ENDPOINTS = {
    BATCHES: "/api/batches",
    SENSORS: "/api/sensors",
    ALERTS: "/api/crop-storage/alerts",
    STORAGE_ENTRIES: "/api/crop-storage/entries",
    STOCK_MOVEMENTS: "/api/stock-movements",
    STORAGE_ZONES: "/api/storage-zones",
    LOSS_RECORDS: "/api/loss-records",
    SENSOR_READINGS: "/api/sensor-readings",
    SUPPLIERS: "/api/suppliers",
    QUALITY_CHECKS: "/api/quality-checks",
};

export function createBatch(batch) {
    const { supplierId, storageZoneId, ...fields } = batch;
    return apiClient.post(ENDPOINTS.BATCHES, {
        ...fields,
        ...(supplierId ? { supplier: { id: Number(supplierId) } } : {}),
        ...(storageZoneId ? { storageZone: { id: Number(storageZoneId) } } : {}),
    });
}

export function fetchBatches() {
    return apiClient.get(ENDPOINTS.BATCHES);
}

export function fetchFifoBatches() {
    return apiClient.get(`${ENDPOINTS.BATCHES}/fifo`);
}

export function fetchBatchSummary() {
    return apiClient.get(`${ENDPOINTS.BATCHES}/summary`);
}

export function fetchExpiringBatches() {
    return apiClient.get(`${ENDPOINTS.BATCHES}/expiring`);
}

export function createSensor(sensor) {
    return apiClient.post(ENDPOINTS.SENSORS, sensor);
}

export function fetchSensors() {
    return apiClient.get(ENDPOINTS.SENSORS);
}

export function createAlert(alert) {
    return apiClient.post(ENDPOINTS.ALERTS, alert);
}

export function fetchAlerts() {
    return apiClient.get(ENDPOINTS.ALERTS);
}

export function fetchAlertsByStatus(status) {
    return apiClient.get(`${ENDPOINTS.ALERTS}/status/${encodeURIComponent(status)}`);
}

export function acknowledgeAlert(id) {
    return apiClient.patch(`${ENDPOINTS.ALERTS}/${encodeURIComponent(id)}/acknowledge`);
}

export function resolveAlert(id) {
    return apiClient.patch(`${ENDPOINTS.ALERTS}/${encodeURIComponent(id)}/resolve`);
}

export function createStorageEntry(entry) {
    return apiClient.post(ENDPOINTS.STORAGE_ENTRIES, entry);
}

export function createStockMovement(movement) {
    const { batchId, movementType, targetZoneId, ...fields } = movement;
    return apiClient.post(ENDPOINTS.STOCK_MOVEMENTS, {
        ...fields,
        type: movementType,
        batch: { id: Number(batchId) },
        ...(targetZoneId ? { toZone: { id: Number(targetZoneId) } } : {}),
    });
}

export function fetchStockMovements() {
    return apiClient.get(ENDPOINTS.STOCK_MOVEMENTS);
}

export function fetchStockMovementsByBatch(batchId) {
    return apiClient.get(
        `${ENDPOINTS.STOCK_MOVEMENTS}/batch/${encodeURIComponent(batchId)}`
    );
}

export function createStorageZone(zone) {
    return apiClient.post(ENDPOINTS.STORAGE_ZONES, zone);
}

export function fetchStorageZones() {
    return apiClient.get(ENDPOINTS.STORAGE_ZONES);
}

export function createLossRecord(record) {
    return apiClient.post(ENDPOINTS.LOSS_RECORDS, record);
}

export function fetchLossRecords() {
    return apiClient.get(ENDPOINTS.LOSS_RECORDS);
}

export function fetchLossRecordsByBatch(batchId) {
    return apiClient.get(`${ENDPOINTS.LOSS_RECORDS}/batch/${encodeURIComponent(batchId)}`);
}

export function createSensorReading(reading) {
    return apiClient.post(ENDPOINTS.SENSOR_READINGS, reading);
}

export function fetchSensorReadings() {
    return apiClient.get(ENDPOINTS.SENSOR_READINGS);
}

export function fetchSensorReadingsBySensor(sensorId) {
    return apiClient.get(
        `${ENDPOINTS.SENSOR_READINGS}/sensor/${encodeURIComponent(sensorId)}`
    );
}

export function createSupplier(supplier) {
    return apiClient.post(ENDPOINTS.SUPPLIERS, supplier);
}

export function fetchSuppliers() {
    return apiClient.get(ENDPOINTS.SUPPLIERS);
}

export function createQualityCheck(check) {
    return apiClient.post(ENDPOINTS.QUALITY_CHECKS, check);
}

export function fetchQualityChecks() {
    return apiClient.get(ENDPOINTS.QUALITY_CHECKS);
}

export function fetchQualityChecksByBatch(batchId) {
    return apiClient.get(`${ENDPOINTS.QUALITY_CHECKS}/batch/${encodeURIComponent(batchId)}`);
}
