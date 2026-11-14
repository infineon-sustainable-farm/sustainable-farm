package com.infineonbit.sustainablefarm.modules.energysupply;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.Alert;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergySource;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.CarbonMetricRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.ConsumptionLogRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyAlertRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.SolarGenerationLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loads the "Base scenario" dummy figures from the Energy Supply Systems
 * Excel business case into PostgreSQL at application startup, so the
 * dashboard has real rows to read instead of hardcoded mock-up values.
 *
 * TODO (Aida): replace the placeholder numbers below with the exact
 * figures from the "TEAM SUMMARY — Energy System" sheet once confirmed
 * (PV capacity in kWp, battery kWh, unit costs in EUR).
 * Seeds each empty table independently, so it is safe to restart the app
 * and can also fill in demo data missing from a partially seeded database.
 */
@Profile("dev")
@Component
public class EnergySupplyDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnergySupplyDataSeeder.class);

    private final EnergyComponentRepository componentRepository;
    private final ConsumptionLogRepository consumptionLogRepository;
    private final CarbonMetricRepository carbonMetricRepository;
    private final GeneratorEventRepository generatorEventRepository;
    private final SolarGenerationLogRepository solarGenerationLogRepository;
    private final EnergyAlertRepository energyAlertRepository;

    public EnergySupplyDataSeeder(EnergyComponentRepository componentRepository,
                                   ConsumptionLogRepository consumptionLogRepository,
                                   CarbonMetricRepository carbonMetricRepository,
                                   GeneratorEventRepository generatorEventRepository,
                                   SolarGenerationLogRepository solarGenerationLogRepository,
                                   EnergyAlertRepository energyAlertRepository) {
        this.componentRepository = componentRepository;
        this.consumptionLogRepository = consumptionLogRepository;
        this.carbonMetricRepository = carbonMetricRepository;
        this.generatorEventRepository = generatorEventRepository;
        this.solarGenerationLogRepository = solarGenerationLogRepository;
        this.energyAlertRepository = energyAlertRepository;
    }

    @Override
    public void run(String... args) {
        if (componentRepository.count() == 0) {
            seedComponents();
        }

        if (consumptionLogRepository.count() == 0) {
            seedTodayConsumption();
        }
        if (carbonMetricRepository.count() == 0) {
            seedThisMonthCarbon();
        }
        if (generatorEventRepository.count() == 0) {
            seedOneGeneratorEvent();
        }
        if (solarGenerationLogRepository.count() == 0) {
            seedSolarHistory();
        }
        if (energyAlertRepository.count() == 0) {
            seedAlerts();
        }
    }

    private void seedComponents() {
        EnergyComponent pv = new EnergyComponent();
        pv.setComponentId("ENG-001");
        pv.setName("Solar PV array (polycrystalline)");
        pv.setCategory(ComponentCategory.PV_ARRAY);
        pv.setCapacityValue(new BigDecimal("10.5")); // TODO: confirm kWp from Excel
        pv.setPurchaseDate(LocalDate.of(2026, 7, 1));
        pv.setUnitCostEur(new BigDecimal("6500.00"));
        pv.setStatus(ComponentStatus.OPERATIONAL);
        pv.setLocation("Banfora farm site");
        componentRepository.save(pv);

        EnergyComponent battery = new EnergyComponent();
        battery.setComponentId("ENG-002");
        battery.setName("Battery storage bank (Li-ion)");
        battery.setCategory(ComponentCategory.BATTERY);
        battery.setCapacityValue(new BigDecimal("20.0")); // kWh, TODO: confirm from Excel
        battery.setPurchaseDate(LocalDate.of(2026, 7, 1));
        battery.setUnitCostEur(new BigDecimal("4200.00"));
        battery.setStatus(ComponentStatus.OPERATIONAL);
        battery.setLocation("Banfora farm site");
        componentRepository.save(battery);

        EnergyComponent inverter = new EnergyComponent();
        inverter.setComponentId("ENG-003");
        inverter.setName("Inverter");
        inverter.setCategory(ComponentCategory.INVERTER);
        inverter.setCapacityValue(new BigDecimal("8.0")); // kW
        inverter.setPurchaseDate(LocalDate.of(2026, 7, 1));
        inverter.setUnitCostEur(new BigDecimal("1100.00"));
        inverter.setStatus(ComponentStatus.OPERATIONAL);
        inverter.setLocation("Banfora farm site");
        componentRepository.save(inverter);

        EnergyComponent generator = new EnergyComponent();
        generator.setComponentId("ENG-004");
        generator.setName("Backup diesel generator");
        generator.setCategory(ComponentCategory.GENERATOR);
        generator.setCapacityValue(new BigDecimal("8.0")); // kW
        generator.setPurchaseDate(LocalDate.of(2026, 7, 1));
        generator.setUnitCostEur(new BigDecimal("4000.00"));
        generator.setStatus(ComponentStatus.OPERATIONAL);
        generator.setLocation("Banfora farm site");
        componentRepository.save(generator);

        EnergyComponent monitoring = new EnergyComponent();
        monitoring.setComponentId("ENG-005");
        monitoring.setName("Monitoring / IoT controller");
        monitoring.setCategory(ComponentCategory.MONITORING);
        monitoring.setCapacityValue(BigDecimal.ZERO);
        monitoring.setPurchaseDate(LocalDate.of(2026, 7, 1));
        monitoring.setUnitCostEur(new BigDecimal("350.00"));
        monitoring.setStatus(ComponentStatus.OPERATIONAL);
        monitoring.setLocation("Banfora farm site");
        componentRepository.save(monitoring);
    }

    private void seedTodayConsumption() {
        LocalDate today = LocalDate.now();

        save(today, LoadCategory.IRRIGATION, new BigDecimal("7.5"), EnergySource.SOLAR);
        save(today, LoadCategory.STORAGE_COOLING, new BigDecimal("8.0"), EnergySource.SOLAR);
        save(today, LoadCategory.LIGHTING, new BigDecimal("2.0"), EnergySource.BATTERY);
        save(today, LoadCategory.SECURITY, new BigDecimal("1.0"), EnergySource.BATTERY);
        save(today, LoadCategory.MONITORING, new BigDecimal("0.5"), EnergySource.BATTERY);
        save(today, LoadCategory.PROCESSING, new BigDecimal("15.0"), EnergySource.SOLAR);
    }

    private void save(LocalDate date, LoadCategory category, BigDecimal kwh, EnergySource source) {
        ConsumptionLog log = new ConsumptionLog();
        log.setDate(date);
        log.setLoadCategory(category);
        log.setKwhConsumed(kwh);
        log.setSource(source);
        consumptionLogRepository.save(log);
    }

    private void seedThisMonthCarbon() {
        CarbonMetric metric = new CarbonMetric();
        metric.setPeriod(LocalDate.now().withDayOfMonth(1));
        metric.setCo2AvoidedKg(new BigDecimal("420.00")); // TODO: confirm from Excel
        metric.setEmissionFactorUsed(new BigDecimal("0.500")); // grid baseline, kg CO2/kWh
        metric.setScenario("Base");
        carbonMetricRepository.save(metric);
    }

    private void seedOneGeneratorEvent() {
        GeneratorEvent event = new GeneratorEvent();
        event.setDate(LocalDate.now());
        event.setTriggerReason("Battery state of charge below 20%");
        event.setDurationHours(new BigDecimal("2.5"));
        event.setFuelLiters(new BigDecimal("4.0"));
        event.setCostEur(new BigDecimal("6.80"));
        generatorEventRepository.save(event);
    }

    private void seedSolarHistory() {
        EnergyComponent pv = findDemoComponent("ENG-001", ComponentCategory.PV_ARRAY);
        if (pv == null) {
            LOGGER.warn("Skipping Energy Supply solar demo data: no PV array component is configured");
            return;
        }
        String[] production = {"38.40", "41.10", "36.80", "44.60", "43.20", "39.70", "45.30"};
        String[] efficiency = {"78.00", "81.00", "74.00", "86.00", "84.00", "79.00", "88.00"};

        for (int day = 0; day < production.length; day++) {
            SolarGenerationLog log = new SolarGenerationLog();
            log.setComponent(pv);
            log.setDate(LocalDate.now().minusDays(production.length - day - 1));
            log.setKwhProduced(new BigDecimal(production[day]));
            log.setEfficiencyPct(new BigDecimal(efficiency[day]));
            solarGenerationLogRepository.save(log);
        }
    }

    private void seedAlerts() {
        EnergyComponent battery = findDemoComponent("ENG-002", ComponentCategory.BATTERY);
        if (battery == null) {
            LOGGER.warn("Skipping Energy Supply alert demo data: no battery component is configured");
            return;
        }

        Alert openAlert = new Alert();
        openAlert.setComponent(battery);
        openAlert.setSeverity(AlertSeverity.WARNING);
        openAlert.setMessage("Battery charge is below the recommended reserve. Monitor overnight consumption.");
        openAlert.setTimestamp(LocalDateTime.now().minusHours(1));
        openAlert.setStatus(AlertStatus.OPEN);
        energyAlertRepository.save(openAlert);

        Alert resolvedAlert = new Alert();
        resolvedAlert.setSeverity(AlertSeverity.INFO);
        resolvedAlert.setMessage("Routine inverter inspection completed.");
        resolvedAlert.setTimestamp(LocalDateTime.now().minusDays(1));
        resolvedAlert.setStatus(AlertStatus.RESOLVED);
        EnergyComponent inverter = findDemoComponent("ENG-003", ComponentCategory.INVERTER);
        if (inverter != null) {
            resolvedAlert.setComponent(inverter);
            energyAlertRepository.save(resolvedAlert);
        } else {
            LOGGER.warn("Skipping resolved Energy Supply demo alert: no inverter component is configured");
        }
    }

    private EnergyComponent findDemoComponent(String componentId, ComponentCategory category) {
        return componentRepository.findById(componentId).orElseGet(() ->
                componentRepository.findAll().stream()
                        .filter(component -> component.getCategory() == category)
                        .findFirst()
                        .orElse(null));
    }
}
