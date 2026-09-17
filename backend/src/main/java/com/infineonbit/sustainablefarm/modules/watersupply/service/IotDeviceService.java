package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotDevice;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotDeviceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des capteurs IoT (P7).
 * Mise à jour du registre à chaque ingestion et calcul de la disponibilité.
 */
@Service
public class IotDeviceService {

    private static final int OFFLINE_THRESHOLD_MINUTES = 15;
    private final IotDeviceRepository deviceRepository;

    public IotDeviceService(IotDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void recordTelemetry(String deviceUid, String type, Instant seenAt,
                                 Integer batteryPercent, Integer rssi) {
        IotDevice device = deviceRepository.findByDeviceUid(deviceUid);
        if (device == null) {
            device = new IotDevice();
            device.setDeviceUid(deviceUid);
            device.setType(type);
        }
        device.setLastSeen(seenAt == null ? Instant.now() : seenAt);
        if (batteryPercent != null && batteryPercent >= 0 && batteryPercent <= 100) {
            device.setBatteryPercent(batteryPercent);
        }
        if (rssi != null) {
            device.setRssi(rssi);
        }
        deviceRepository.save(device);
    }

    public Map<String, Object> availability() {
        long total = deviceRepository.countAll();
        if (total == 0) {
            return Map.of("availabilityPercentage", 100, "total", 0, "online", 0, "offline", 0);
        }
        Instant cutoff = Instant.now().minus(OFFLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<IotDevice> offline = deviceRepository.findOfflineSince(cutoff);
        long offlineCount = (long) offline.size();
        long online = total - offlineCount;
        double pct = Math.round(((double) online / total) * 10000) / 100.0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availabilityPercentage", pct);
        result.put("total", total);
        result.put("online", online);
        result.put("offline", offlineCount);
        result.put("offlineSinceMinutes", offline.stream().mapToLong(d -> {
            if (d.getLastSeen() == null) return Integer.MAX_VALUE;
            return ChronoUnit.MINUTES.between(d.getLastSeen(), Instant.now());
        }).min().orElse(Integer.MAX_VALUE));
        return result;
    }

    public List<Map<String, Object>> listDevices() {
        Instant cutoff = Instant.now().minus(OFFLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        return deviceRepository.findAll().stream()
                .map(d -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", d.getId());
                    item.put("device_uid", d.getDeviceUid());
                    item.put("type", d.getType());
                    item.put("last_seen", d.getLastSeen() == null ? null : d.getLastSeen().toString());
                    item.put("battery_percent", d.getBatteryPercent());
                    item.put("rssi", d.getRssi());
                    item.put("status", d.getLastSeen() == null || d.getLastSeen().isBefore(cutoff)
                            ? "offline" : "online");
                    return item;
                })
                .toList();
    }

    public long countAll() {
        return deviceRepository.countAll();
    }
}
