package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
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

    /** Date de génération du rapport : toujours l'instant courant (corrige un bug B4 où la date était figée au démarrage). */
    private Instant reportGeneratedAt() {
        return Instant.now();
    }

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
        sb.append("Genere le : ").append(reportGeneratedAt()).append("\n\n");
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
        sb.append("Genere le : ").append(reportGeneratedAt()).append("\n\n");
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
        sb.append("Genere le : ").append(reportGeneratedAt()).append("\n\n");
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

    // ==================== P10 : Exports CSV ====================

    /**
     * Export CSV des consommations de la periode (columns : date, source_id, litres, farm_id).
     * Format preuve pour le suivi des economies d'eau : reimportable dans un tableur.
     */
    @GetMapping("/consumption/csv")
    public ResponseEntity<byte[]> consumptionCsv(@RequestParam(defaultValue = "month") String period) {
        var range = period(period);
        List<WaterConsumption> consumptions = waterConsumptionRepository.findAll().stream()
                .filter(c -> c.getConsumptionDate() != null
                        && !c.getConsumptionDate().isBefore(range.start)
                        && c.getConsumptionDate().isBefore(range.end))
                .sorted(Comparator.comparing(WaterConsumption::getConsumptionDate))
                .toList();

        List<String[]> rows = new ArrayList<>();
        double total = 0;
        for (WaterConsumption c : consumptions) {
            double liters = c.getConsumptionLiters() == null ? 0 : c.getConsumptionLiters();
            total += liters;
            rows.add(new String[] {
                    c.getConsumptionDate().toString(),
                    string(c.getSourceId()),
                    String.valueOf(liters),
                    string(c.getFarmId()) });
        }
        rows.add(new String[] { "TOTAL", "", formatFr(total), "" });
        return csv("consommation-" + period + ".csv",
                new String[] { "date", "source_id", "litres", "farm_id" }, rows);
    }

    /** Export CSV des irrigations planifiees de la periode. */
    @GetMapping("/irrigation/csv")
    public ResponseEntity<byte[]> irrigationCsv(@RequestParam(defaultValue = "month") String period) {
        var range = period(period);
        var schedules = irrigationScheduleRepository.findAll().stream()
                .filter(s -> s.getStartTime() != null
                        && !s.getStartTime().isBefore(range.start)
                        && s.getStartTime().isBefore(range.end))
                .sorted(Comparator.comparing(IrrigationSchedule::getStartTime))
                .toList();

        List<String[]> rows = new ArrayList<>();
        double planned = 0;
        for (var s : schedules) {
            double liters = s.getWaterQuantityLiters() == null ? 0 : s.getWaterQuantityLiters();
            planned += liters;
            rows.add(new String[] {
                    s.getStartTime().toString(),
                    string(s.getZoneId()),
                    s.getDurationMinutes() == null ? "" : String.valueOf(s.getDurationMinutes()),
                    String.valueOf(liters),
                    string(s.getStatus()) });
        }
        rows.add(new String[] { "TOTAL PLANIFIE", "", "", formatFr(planned), "" });
        return csv("irrigation-" + period + ".csv",
                new String[] { "debut", "zone_id", "duree_min", "litres", "statut" }, rows);
    }

    /** Export CSV des tests de qualite de l'eau (toutes periodes). */
    @GetMapping("/quality/csv")
    public ResponseEntity<byte[]> qualityCsv() {
        List<WaterQualityTest> tests = waterQualityTestRepository.findAll().stream()
                .sorted(Comparator.comparing(WaterQualityTest::getTestDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        List<String[]> rows = new ArrayList<>();
        for (WaterQualityTest t : tests) {
            boolean phOut = t.getPh() != null && (t.getPh() < 6.0 || t.getPh() > 7.5);
            boolean turb = t.getTurbidityNtu() != null && t.getTurbidityNtu() > 5.0;
            rows.add(new String[] {
                    t.getTestDate() == null ? "" : t.getTestDate().toString(),
                    string(t.getSourceId()),
                    t.getPh() == null ? "" : formatFr(t.getPh()),
                    t.getTurbidityNtu() == null ? "" : formatFr(t.getTurbidityNtu()),
                    (phOut || turb) ? "NON-CONFORME" : "Conforme" });
        }
        return csv("qualite-eau.csv",
                new String[] { "date", "source_id", "ph", "turbidite_ntu", "statut" }, rows);
    }

    /**
     * Genere la reponse CSV (separateur point-virgule, compatible Excel FR ; BOM UTF-8 pour
     * l'ouverture directe avec les accents corrects).
     */
    private ResponseEntity<byte[]> csv(String filename, String[] headers, List<String[]> rows) {
        StringJoiner joiner = new StringJoiner("\r\n");
        joiner.add(encodeCsvRow(headers));
        for (String[] row : rows) {
            joiner.add(encodeCsvRow(row));
        }
        String body = joiner + "\r\n";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bytes.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(bytes, 0, withBom, 3, bytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(withBom);
    }

    /** Echappe une ligne CSV (guillemets, separateur, retours ligne). */
    private String encodeCsvRow(String[] cells) {
        StringJoiner row = new StringJoiner(";");
        for (String cell : cells) {
            String value = cell == null ? "" : cell;
            boolean mustQuote = value.contains(";") || value.contains("\"")
                    || value.contains("\n") || value.contains("\r");
            row.add(mustQuote ? '"' + value.replace("\"", "\"\"") + '"' : value);
        }
        return row.toString();
    }

    /** Valeur textuelle tolérante (null -> cellule vide) pour les colonnes CSV non numériques. */
    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    /** Formate un nombre avec la virgule decimale Francaise (tableurs FR). */
    private String formatFr(double value) {
        return String.format(java.util.Locale.FRANCE, "%.2f", value);
    }

    private record InstantPeriod(java.time.Instant start, java.time.Instant end) {
    }
}
