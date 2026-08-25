package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generation de rapports exportables (TXT) pour l'analyse et le partage.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final WaterConsumptionRepository waterConsumptionRepository;
    private final IrrigationScheduleRepository irrigationScheduleRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;

    public ReportController(WaterConsumptionRepository waterConsumptionRepository,
                            IrrigationScheduleRepository irrigationScheduleRepository,
                            WaterQualityTestRepository waterQualityTestRepository) {
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.irrigationScheduleRepository = irrigationScheduleRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
    }

    private InstantPeriod period(String p) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String key = p == null ? "month" : p.toLowerCase();
        LocalDate start = switch (key) {
            case "day" -> today;
            case "week" -> today.minusDays(6);
            default -> today.withDayOfMonth(1);
        };
        return new InstantPeriod(start.atStartOfDay().toInstant(ZoneOffset.UTC), today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private java.time.Instant now = java.time.Instant.now();

    @GetMapping("/consumption")
    public ResponseEntity<byte[]> consumptionReport(@RequestParam(defaultValue = "month") String period) {
        var range = period(period);
        List<WaterConsumption> consumptions = waterConsumptionRepository.findAll().stream()
                .filter(c -> c.getConsumptionDate() != null
                        && !c.getConsumptionDate().isBefore(range.start)
                        && c.getConsumptionDate().isBefore(range.end))
                .sorted(Comparator.comparing(WaterConsumption::getConsumptionDate))
                .toList();

        double total = consumptions.stream()
                .mapToDouble(c -> c.getConsumptionLiters() == null ? 0 : c.getConsumptionLiters())
                .sum();

        StringBuilder sb = new StringBuilder();
        sb.append("=== RAPPORT DE CONSOMMATION D'EAU ===\n");
        sb.append("Periode : ").append(period).append("\n");
        sb.append("Genere le : ").append(now).append("\n\n");
        sb.append("Date | Source | Litres\n");
        sb.append("--------------------------------\n");
        for (WaterConsumption c : consumptions) {
            sb.append(c.getConsumptionDate()).append(" | ")
              .append(c.getSourceId()).append(" | ")
              .append(c.getConsumptionLiters()).append(" L\n");
        }
        sb.append("--------------------------------\n");
        sb.append("TOTAL : ").append(String.format("%.2f", total)).append(" L\n");

        return txt("consumption-report-" + period + ".txt", sb.toString());
    }

    @GetMapping("/irrigation")
    public ResponseEntity<byte[]> irrigationReport(@RequestParam(defaultValue = "month") String period) {
        var range = period(period);
        var schedules = irrigationScheduleRepository.findAll().stream()
                .filter(s -> s.getStartTime() != null
                        && !s.getStartTime().isBefore(range.start)
                        && s.getStartTime().isBefore(range.end))
                .toList();

        double planned = schedules.stream()
                .mapToDouble(s -> s.getWaterQuantityLiters() == null ? 0 : s.getWaterQuantityLiters())
                .sum();

        StringBuilder sb = new StringBuilder();
        sb.append("=== RAPPORT D'IRRIGATION ===\n");
        sb.append("Periode : ").append(period).append("\n");
        sb.append("Genere le : ").append(now).append("\n\n");
        sb.append("Zone | Debut | Duree | Volume | Statut\n");
        sb.append("-------------------------------------------\n");
        for (var s : schedules) {
            sb.append(s.getZoneId()).append(" | ")
              .append(s.getStartTime()).append(" | ")
              .append(s.getDurationMinutes()).append(" min | ")
              .append(s.getWaterQuantityLiters()).append(" L | ")
              .append(s.getStatus()).append("\n");
        }
        sb.append("-------------------------------------------\n");
        sb.append("VOLUME PLANIFIE : ").append(String.format("%.2f", planned)).append(" L\n");

        return txt("irrigation-report-" + period + ".txt", sb.toString());
    }

    @GetMapping("/quality")
    public ResponseEntity<byte[]> qualityReport() {
        List<WaterQualityTest> tests = waterQualityTestRepository.findAll().stream()
                .sorted(Comparator.comparing(WaterQualityTest::getTestDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("=== RAPPORT DE QUALITE DE L'EAU ===\n");
        sb.append("Genere le : ").append(now).append("\n\n");
        sb.append("Source | pH | Turbidite | Statut\n");
        sb.append("------------------------------------\n");
        for (WaterQualityTest t : tests) {
            boolean phOut = t.getPh() != null && (t.getPh() < 6.0 || t.getPh() > 7.5);
            boolean turb = t.getTurbidityNtu() != null && t.getTurbidityNtu() > 5.0;
            String statut = (phOut || turb) ? "NON-CONFORME" : "Conforme";
            sb.append(t.getSourceId()).append(" | ")
              .append(t.getPh()).append(" | ")
              .append(t.getTurbidityNtu()).append(" NTU | ")
              .append(statut).append("\n");
        }

        return txt("quality-report.txt", sb.toString());
    }

    private ResponseEntity<byte[]> txt(String filename, String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    private record InstantPeriod(java.time.Instant start, java.time.Instant end) {
    }
}
