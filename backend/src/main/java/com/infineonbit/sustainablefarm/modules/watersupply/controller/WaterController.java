package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/water")
public class WaterController {
    private final WaterSourceRepository waterSourceRepository;
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public WaterController(
            WaterSourceRepository waterSourceRepository,
            WaterConsumptionRepository waterConsumptionRepository,
            WaterQualityTestRepository waterQualityTestRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.waterSourceRepository = waterSourceRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/sources")
    public List<WaterSource> sources() {
        return waterSourceRepository.findAll();
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterSource createSource(@Valid @RequestBody WaterSource source) {
        return waterSourceRepository.save(source);
    }

    @GetMapping("/sources/{sourceId}")
    public WaterSource source(@PathVariable UUID sourceId) {
        return waterSourceRepository.findById(sourceId).orElseThrow(() -> new NotFoundException("WaterSource"));
    }

    @PutMapping("/sources/{sourceId}")
    public WaterSource updateSource(@PathVariable UUID sourceId, @RequestBody WaterSource payload) {
        WaterSource source = waterSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("WaterSource"));
        source.setFarmId(payload.getFarmId() == null ? source.getFarmId() : payload.getFarmId());
        source.setName(payload.getName() == null ? source.getName() : payload.getName());
        source.setType(payload.getType() == null ? source.getType() : payload.getType());
        source.setCapacityLiters(payload.getCapacityLiters() == null ? source.getCapacityLiters() : payload.getCapacityLiters());
        source.setCurrentLevelLiters(payload.getCurrentLevelLiters() == null ? source.getCurrentLevelLiters() : payload.getCurrentLevelLiters());
        source.setLatitude(payload.getLatitude());
        source.setLongitude(payload.getLongitude());
        return waterSourceRepository.save(source);
    }

    @DeleteMapping("/sources/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable UUID sourceId) {
        waterSourceRepository.delete(waterSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("WaterSource")));
    }

    @GetMapping("/consumption")
    public List<WaterConsumption> consumption() {
        return waterConsumptionRepository.findAll();
    }

    @PostMapping("/consumption")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterConsumption createConsumption(@Valid @RequestBody WaterConsumption consumption) {
        return waterConsumptionRepository.save(consumption);
    }

    @GetMapping("/consumption/{consumptionId}")
    public WaterConsumption consumption(@PathVariable UUID consumptionId) {
        return waterConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new NotFoundException("WaterConsumption"));
    }

    @PutMapping("/consumption/{consumptionId}")
    public WaterConsumption updateConsumption(@PathVariable UUID consumptionId, @RequestBody WaterConsumption payload) {
        WaterConsumption consumption = waterConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new NotFoundException("WaterConsumption"));
        consumption.setFarmId(payload.getFarmId() == null ? consumption.getFarmId() : payload.getFarmId());
        consumption.setSourceId(payload.getSourceId() == null ? consumption.getSourceId() : payload.getSourceId());
        consumption.setConsumptionLiters(payload.getConsumptionLiters() == null ? consumption.getConsumptionLiters() : payload.getConsumptionLiters());
        consumption.setConsumptionDate(payload.getConsumptionDate() == null ? consumption.getConsumptionDate() : payload.getConsumptionDate());
        consumption.setIrrigationId(payload.getIrrigationId() == null ? consumption.getIrrigationId() : payload.getIrrigationId());
        return waterConsumptionRepository.save(consumption);
    }

    @DeleteMapping("/consumption/{consumptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConsumption(@PathVariable UUID consumptionId) {
        waterConsumptionRepository.delete(waterConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new NotFoundException("WaterConsumption")));
    }

    @GetMapping("/quality")
    public List<WaterQualityTest> quality() {
        return waterQualityTestRepository.findAll();
    }

    /**
     * Cree un test qualite et genere automatiquement une Notification de type
     * "warning" si un parametre sort des plages cibles (pH hors [6.0, 7.5],
     * turbidite > 5 NTU.
     */
    @PostMapping("/quality")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterQualityTest createQualityTest(@Valid @RequestBody WaterQualityTest test) {
        WaterQualityTest saved = waterQualityTestRepository.save(test);
        checkQualityAndAlert(saved);
        return saved;
    }

    @GetMapping("/quality/{qualityTestId}")
    public WaterQualityTest qualityTest(@PathVariable UUID qualityTestId) {
        return waterQualityTestRepository.findById(qualityTestId)
                .orElseThrow(() -> new NotFoundException("WaterQualityTest"));
    }

    @PutMapping("/quality/{qualityTestId}")
    public WaterQualityTest updateQualityTest(@PathVariable UUID qualityTestId, @RequestBody WaterQualityTest payload) {
        WaterQualityTest test = waterQualityTestRepository.findById(qualityTestId)
                .orElseThrow(() -> new NotFoundException("WaterQualityTest"));
        test.setSourceId(payload.getSourceId() == null ? test.getSourceId() : payload.getSourceId());
        test.setPh(payload.getPh());
        test.setTemperatureCelsius(payload.getTemperatureCelsius());
        test.setTurbidityNtu(payload.getTurbidityNtu());
        test.setConductivityUsCm(payload.getConductivityUsCm());
        test.setSalinityPpt(payload.getSalinityPpt());
        test.setTestDate(payload.getTestDate() == null ? test.getTestDate() : payload.getTestDate());
        WaterQualityTest updated = waterQualityTestRepository.save(test);
        checkQualityAndAlert(updated);
        return updated;
    }

    @DeleteMapping("/quality/{qualityTestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQualityTest(@PathVariable UUID qualityTestId) {
        waterQualityTestRepository.delete(waterQualityTestRepository.findById(qualityTestId)
                .orElseThrow(() -> new NotFoundException("WaterQualityTest")));
    }

    /**
     * Regle metier : si pH hors [6.0, 7.5] OU turbidite > 5 NTU, creer une
     * notification "warning" (alerte hors-norme).
     */
    private void checkQualityAndAlert(WaterQualityTest test) {
        boolean phOutOfRange = test.getPh() != null && (test.getPh() < 6.0 || test.getPh() > 7.5);
        boolean turbidityHigh = test.getTurbidityNtu() != null && test.getTurbidityNtu() > 5.0;
        if (!phOutOfRange && !turbidityHigh) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(defaultUserId());
        notification.setTitle("Eau non conforme");
        notification.setMessage(buildQualityMessage(test, phOutOfRange, turbidityHigh));
        notification.setType("warning");
        notification.setRead(false);
        notification.setActionUrl("/api/water/quality/" + test.getId());
        notificationRepository.save(notification);
    }

    private String buildQualityMessage(WaterQualityTest test, boolean phOut, boolean turbidityHigh) {
        StringBuilder sb = new StringBuilder("Test qualité hors-norme sur la source " + test.getSourceId() + " : ");
        if (phOut) {
            sb.append("pH=").append(test.getPh()).append(" (hors 6.0-7.5) ");
        }
        if (turbidityHigh) {
            sb.append("turbidité=").append(test.getTurbidityNtu()).append(" NTU (> 5) ");
        }
        return sb.toString().trim();
    }

    private UUID defaultUserId() {
        return userRepository.findAll().stream()
                .findFirst()
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalStateException("Aucun utilisateur disponible pour la notification"));
    }
}
