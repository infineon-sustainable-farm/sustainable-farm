package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorReadingRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business service for sensor readings: persists each measure and checks it
 * against the thresholds of the storage zone the sensor belongs to.
 */
@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final SensorRepository sensorRepository;
    private final AlertRepository alertRepository;

    /**
     * Saves a new sensor reading, then compares it with the temperature and
     * humidity thresholds of the zone where the sensor is installed and raises
     * an ACTIVE alert when a value is out of range.
     *
     * @param reading reading parsed from the request body; only its sensor id is trusted
     * @return the saved reading with its generated id
     */
    @Transactional
    public SensorReading create(SensorReading reading) {
        SensorReading saved = sensorReadingRepository.save(reading);
        checkThresholds(saved);
        return saved;
    }

    /**
     * Returns the full reading history across all sensors, unsorted.
     *
     * @return every stored reading; prefer {@link #findBySensor(Long)} for one sensor
     */
    public List<SensorReading> findAll() {
        return sensorReadingRepository.findAll();
    }

    /**
     * Returns the history of one sensor, most recent measure first.
     *
     * @param sensorId id of the sensor to filter on
     * @return readings of that sensor, newest first
     */
    public List<SensorReading> findBySensor(Long sensorId) {
        return sensorReadingRepository.findBySensorIdOrderByMeasuredAtDesc(sensorId);
    }

    /**
     * Compares one reading with the four thresholds of its zone (temperature
     * min/max, humidity min/max) and raises one alert per crossed bound.
     * Seed data uses the dried-mango storage range: 10 to 25 C and 20 to 60 %.
     */
    private void checkThresholds(SensorReading reading) {
        // Reload the sensor from the database: never trust the object received.
        Sensor sensor = sensorRepository.findById(reading.getSensor().getId()).orElse(null);
        if (sensor == null || sensor.getStorageZone() == null) {
            return;
        }
        StorageZone zone = sensor.getStorageZone();

        // Thresholds in local variables to keep the comparisons readable.
        BigDecimal tMax = zone.getTemperatureMaxCelsius();
        BigDecimal tMin = zone.getTemperatureMinCelsius();
        BigDecimal hMax = zone.getHumidityMaxPercent();
        BigDecimal hMin = zone.getHumidityMinPercent();

        if (reading.getTemperature() != null) {
            if (tMax != null && reading.getTemperature().compareTo(tMax) > 0) {
                raiseAlert("TEMPERATURE",
                        "Temperature " + plain(reading.getTemperature()) + " C above max "
                                + plain(tMax) + " C in zone " + zone.getZoneCode(),
                        sensor, zone);
            } else if (tMin != null && reading.getTemperature().compareTo(tMin) < 0) {
                raiseAlert("TEMPERATURE",
                        "Temperature " + plain(reading.getTemperature()) + " C below min "
                                + plain(tMin) + " C in zone " + zone.getZoneCode(),
                        sensor, zone);
            }
        }

        if (reading.getHumidity() != null) {
            if (hMax != null && reading.getHumidity().compareTo(hMax) > 0) {
                raiseAlert("HUMIDITY",
                        "Humidity " + plain(reading.getHumidity()) + " % above max "
                                + plain(hMax) + " % in zone " + zone.getZoneCode(),
                        sensor, zone);
            } else if (hMin != null && reading.getHumidity().compareTo(hMin) < 0) {
                raiseAlert("HUMIDITY",
                        "Humidity " + plain(reading.getHumidity()) + " % below min "
                                + plain(hMin) + " % in zone " + zone.getZoneCode(),
                        sensor, zone);
            }
        }
    }

    /**
     * Builds and saves one HIGH / ACTIVE alert linked to the sensor and zone.
     */
    private void raiseAlert(String type, String message, Sensor sensor, StorageZone zone) {
        Alert alert = new Alert();
        alert.setType(type);
        alert.setSeverity("HIGH");
        alert.setStatus("ACTIVE");
        alert.setMessage(message);
        alert.setSensor(sensor);
        alert.setStorageZone(zone);
        alertRepository.save(alert);
    }

    /**
     * Renders a numeric column value without its scale zeros, so alert
     * messages read 25 instead of 25.00.
     */
    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}