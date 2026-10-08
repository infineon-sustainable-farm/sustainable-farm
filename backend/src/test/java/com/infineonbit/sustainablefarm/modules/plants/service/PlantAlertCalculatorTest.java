package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertCalculator.HarvestWindow;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertCalculator.Season;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlantAlertCalculatorTest {

    private static Season season(String start, String end) {
        return new Season(LocalDate.parse(start), LocalDate.parse(end));
    }

    private static HarvestWindow window(String start, String end) {
        return new HarvestWindow(LocalDate.parse(start), LocalDate.parse(end));
    }

    /** An alert whose message names it, with its row in the field of its type. */
    private static PlantAlert alert(String name, PlantAlertType type, PlantAlertSeverity severity, String date,
                                    Integer farmId, String blockCode, String varietyName, long id) {
        return new PlantAlert(type, severity, farmId, blockCode, varietyName, name,
                date == null ? null : LocalDate.parse(date), null, null,
                type == PlantAlertType.HARVEST_APPROACHING ? id : null,
                type == PlantAlertType.PRE_HARVEST_INTERVAL ? id : null,
                type == PlantAlertType.OPEN_HEALTH_ISSUE ? id : null,
                type == PlantAlertType.LOW_STOCK ? id : null,
                type == PlantAlertType.NURSERY_READY ? id : null,
                null);
    }

    /** The names of the alerts, once sorted from the reverse of the given order. */
    private static List<String> sorted(PlantAlert... alerts) {
        List<PlantAlert> list = new ArrayList<>(List.of(alerts));
        Collections.reverse(list);
        list.sort(PlantAlertCalculator.ORDER);
        return list.stream().map(PlantAlert::message).toList();
    }

    @Test
    void currentOrNextSeason_shouldBeTheSeasonUnderWay_fromItsFirstToItsLastDay() {
        Season keitt2027 = season("2027-05-01", "2027-07-31");
        assertEquals(keitt2027, PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 5, 1), 5, 7));
        assertEquals(keitt2027, PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 5, 15), 5, 7));
        assertEquals(keitt2027, PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 7, 31), 5, 7));
    }

    @Test
    void currentOrNextSeason_shouldBeTheNextOne_beforeAndAfterTheSeasonOfTheYear() {
        assertEquals(season("2027-05-01", "2027-07-31"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 4, 1), 5, 7));
        assertEquals(season("2028-05-01", "2028-07-31"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 8, 1), 5, 7));
    }

    @Test
    void currentOrNextSeason_shouldRunOverTheNewYear() {
        // November to February: under way in January, next in October and in March
        assertEquals(season("2026-11-01", "2027-02-28"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 1, 15), 11, 2));
        assertEquals(season("2027-11-01", "2028-02-29"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2028, 1, 15), 11, 2));
        assertEquals(season("2027-11-01", "2028-02-29"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 10, 15), 11, 2));
        assertEquals(season("2027-11-01", "2028-02-29"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 3, 1), 11, 2));
    }

    @Test
    void currentOrNextSeason_shouldHandleSeasonsOfOneAndOfTwelveMonths() {
        assertEquals(season("2027-06-01", "2027-06-30"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 6, 10), 6, 6));
        assertEquals(season("2028-06-01", "2028-06-30"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 7, 1), 6, 6));
        // Twelve months: always under way, from the last first month
        assertEquals(season("2027-01-01", "2027-12-31"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 8, 20), 1, 12));
        assertEquals(season("2026-05-01", "2027-04-30"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 3, 10), 5, 4));
        assertEquals(season("2027-05-01", "2028-04-30"),
                PlantAlertCalculator.currentOrNextSeason(LocalDate.of(2027, 5, 1), 5, 4));
    }

    @Test
    void months_shouldListTheMonthsOfASeasonOverTheNewYear() {
        assertEquals(List.of(YearMonth.of(2027, 11), YearMonth.of(2027, 12), YearMonth.of(2028, 1),
                        YearMonth.of(2028, 2)),
                season("2027-11-01", "2028-02-29").months());
        assertEquals(season("2028-11-01", "2029-02-28"),
                PlantAlertCalculator.nextYear(season("2027-11-01", "2028-02-29")));
    }

    @Test
    void harvestWindow_shouldStartWithTheSeason_whenItsFirstMonthIsProductive() {
        assertEquals(Optional.of(window("2027-05-01", "2027-07-31")),
                PlantAlertCalculator.harvestWindow(LocalDate.of(2027, 4, 1), 5, 7, month -> true));
    }

    @Test
    void harvestWindow_shouldStartOnTheFirstProductiveMonth_whenTheTreesComeOfAgeDuringTheSeason() {
        // The trees give a yield from July: before and during the season, the window starts on 1 July
        assertEquals(Optional.of(window("2027-07-01", "2027-07-31")),
                PlantAlertCalculator.harvestWindow(LocalDate.of(2027, 6, 15), 5, 7,
                        month -> !month.isBefore(YearMonth.of(2027, 7))));
        assertEquals(Optional.of(window("2027-07-01", "2027-07-31")),
                PlantAlertCalculator.harvestWindow(LocalDate.of(2027, 4, 1), 5, 7,
                        month -> !month.isBefore(YearMonth.of(2027, 7))));
    }

    @Test
    void harvestWindow_shouldBeEmpty_whenNoMonthOfTheNextSeasonIsProductive() {
        // Too young for the whole 2026 season; the 2027 one, more than a year ahead, is not looked at
        assertEquals(Optional.empty(), PlantAlertCalculator.harvestWindow(LocalDate.of(2026, 4, 1), 5, 7,
                month -> !month.isBefore(YearMonth.of(2027, 5))));
    }

    @Test
    void harvestWindow_shouldTakeTheNextSeason_whenNoMonthOfTheSeasonUnderWayIsProductive() {
        // A January to November season, trees productive from January 2028, on 15 November 2027
        assertEquals(Optional.of(window("2028-01-01", "2028-11-30")),
                PlantAlertCalculator.harvestWindow(LocalDate.of(2027, 11, 15), 1, 11,
                        month -> !month.isBefore(YearMonth.of(2028, 1))));
        // The same in May 2026, when no month of 2027 counts either
        assertEquals(Optional.empty(), PlantAlertCalculator.harvestWindow(LocalDate.of(2026, 5, 15), 5, 7,
                month -> !month.isBefore(YearMonth.of(2028, 5))));
    }

    @Test
    void isOpenOn_shouldHoldTheFirstAndTheLastDay() {
        HarvestWindow july = window("2027-07-01", "2027-07-31");
        assertFalse(july.isOpenOn(LocalDate.of(2027, 6, 30)));
        assertTrue(july.isOpenOn(LocalDate.of(2027, 7, 1)));
        assertTrue(july.isOpenOn(LocalDate.of(2027, 7, 31)));
        assertFalse(july.isOpenOn(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void daysFromToday_shouldBePositiveBeforeTheDate_andNegativeAfterIt() {
        assertEquals(30, PlantAlertCalculator.daysFromToday(LocalDate.of(2027, 4, 1), LocalDate.of(2027, 5, 1)));
        assertEquals(274, PlantAlertCalculator.daysFromToday(LocalDate.of(2027, 8, 1), LocalDate.of(2028, 5, 1)));
        assertEquals(0, PlantAlertCalculator.daysFromToday(LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 1)));
        assertEquals(-12, PlantAlertCalculator.daysFromToday(LocalDate.of(2027, 5, 15), LocalDate.of(2027, 5, 3)));
    }

    @Test
    void relativeDays_shouldWordTheDistanceFromToday() {
        assertEquals("in 30 days", PlantAlertCalculator.relativeDays(30));
        assertEquals("in 1 day", PlantAlertCalculator.relativeDays(1));
        assertEquals("today", PlantAlertCalculator.relativeDays(0));
        assertEquals("1 day ago", PlantAlertCalculator.relativeDays(-1));
        assertEquals("12 days ago", PlantAlertCalculator.relativeDays(-12));
    }

    @Test
    void formatDate_shouldWriteTheDayTheMonthNameAndTheYear() {
        assertEquals("1 May 2027", PlantAlertCalculator.formatDate(LocalDate.of(2027, 5, 1)));
        assertEquals("29 February 2028", PlantAlertCalculator.formatDate(LocalDate.of(2028, 2, 29)));
    }

    @Test
    void stageName_shouldBeInLowerCaseWords() {
        assertEquals("ready to transplant", PlantAlertCalculator.stageName(NurseryStage.READY_TO_TRANSPLANT));
        assertEquals("hardening", PlantAlertCalculator.stageName(NurseryStage.HARDENING));
    }

    @Test
    void order_shouldPutCriticalFirst_thenTheEarliestDate_andTheAlertsWithoutDateLast() {
        assertEquals(List.of("critical interval", "critical stock", "season", "issue", "batch", "stock"), sorted(
                alert("season", PlantAlertType.HARVEST_APPROACHING, PlantAlertSeverity.WARNING, "2027-05-01",
                        5, "C", "Keitt", 1),
                alert("critical stock", PlantAlertType.LOW_STOCK, PlantAlertSeverity.CRITICAL, null,
                        null, null, null, 2),
                alert("critical interval", PlantAlertType.PRE_HARVEST_INTERVAL, PlantAlertSeverity.CRITICAL,
                        "2027-05-22", 5, "C", null, 3),
                alert("issue", PlantAlertType.OPEN_HEALTH_ISSUE, PlantAlertSeverity.WARNING, "2027-05-03",
                        5, "C", null, 4),
                alert("batch", PlantAlertType.NURSERY_READY, PlantAlertSeverity.WARNING, "2027-06-15",
                        5, "D", "Keitt", 5),
                alert("stock", PlantAlertType.LOW_STOCK, PlantAlertSeverity.WARNING, null,
                        null, null, null, 6)));
    }

    @Test
    void order_shouldBreakTiesByTypeFarmBlockVarietyThenRow() {
        PlantAlertSeverity warning = PlantAlertSeverity.WARNING;
        assertEquals(List.of("B Amelie", "C Amelie", "C Keitt 1", "C Keitt 2", "farm 1", "interval", "batch"), sorted(
                alert("C Keitt 2", PlantAlertType.HARVEST_APPROACHING, warning, "2027-05-01", null, "C", "Keitt", 2),
                alert("batch", PlantAlertType.NURSERY_READY, warning, "2027-05-01", null, null, "Keitt", 7),
                alert("farm 1", PlantAlertType.HARVEST_APPROACHING, warning, "2027-05-01", 1, "A", "Kent", 3),
                alert("C Keitt 1", PlantAlertType.HARVEST_APPROACHING, warning, "2027-05-01", null, "C", "Keitt", 1),
                alert("interval", PlantAlertType.PRE_HARVEST_INTERVAL, warning, "2027-05-01", null, "A", null, 6),
                alert("C Amelie", PlantAlertType.HARVEST_APPROACHING, warning, "2027-05-01", null, "C", "Amelie", 5),
                alert("B Amelie", PlantAlertType.HARVEST_APPROACHING, warning, "2027-05-01", null, "B", "Amelie", 4)));
    }
}
