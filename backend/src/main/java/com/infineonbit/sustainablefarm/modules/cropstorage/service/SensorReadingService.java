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

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final SensorRepository sensorRepository;
    private final AlertRepository alertRepository;

    @Transactional
    public SensorReading create(SensorReading reading) {
        SensorReading saved = sensorReadingRepository.save(reading);
        checkThresholds(saved);
        return saved;
    }

    public List<SensorReading> findAll() {
        return sensorReadingRepository.findAll();
    }

    public List<SensorReading> findBySensor(Long sensorId) {
        return sensorReadingRepository.findBySensorIdOrderByMeasuredAtDesc(sensorId);
    }

    // Compare la mesure aux seuils de la zone du capteur et leve une alerte si depassement.
    private void checkThresholds(SensorReading reading) {
        // On relit le capteur depuis la base : ne jamais faire confiance a l'objet recu.
        Sensor sensor = sensorRepository.findById(reading.getSensor().getId()).orElse(null);
        if (sensor == null || sensor.getStorageZone() == null) {
            return;
        }
        StorageZone zone = sensor.getStorageZone();

        // Seuils lus en variables locales pour que les comparaisons restent lisibles.
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

    // Les seuils sortent de colonnes numeric a echelle 2 (25.00) ; on retire les zeros
    // de queue pour que le message affiche 25 au lieu de 25.00.
    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}