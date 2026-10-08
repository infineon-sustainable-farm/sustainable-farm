package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills, at startup in the dev profile, the natural keys of the variety and
 * calendar rows written before the keys existed, and reports the duplicates it
 * finds.
 *
 * <p>Each row is filled in its own transaction, with a bulk update that
 * changes no other column: {@code date_maj} keeps its value. When a row has
 * the same key as an older one, the unique constraint refuses it: its key
 * stays empty and it is logged as an error with both identifiers. Nothing is
 * ever deleted or merged; that is left to a person who knows the orchard.
 *
 * <p>Runs in the dev profile only, like {@link CurrencyRateLoader}. In another
 * profile, the rows written before the keys existed keep an empty key until
 * they are updated, and their duplicates are not reported.
 *
 * <p>It never stops the startup: any failure is logged and the application
 * starts anyway. The rows left without a key are still found by the planting
 * lookups, which read the columns, not the key.
 */
@Component
@Profile("dev")
public class PlantingKeyBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlantingKeyBackfill.class);

    /**
     * What one filling did.
     *
     * @param varietiesKeyed     number of variety rows given a key
     * @param varietyDuplicates  variety rows left without a key, as duplicates of an older row
     * @param calendarsKeyed     number of calendar rows given a key
     * @param calendarDuplicates calendar rows left without a key, as duplicates of an older row
     */
    record Result(int varietiesKeyed, List<Long> varietyDuplicates,
                  int calendarsKeyed, List<Long> calendarDuplicates) {
    }

    private final VarietyRepository varietyRepository;
    private final GrowthCalendarRepository growthCalendarRepository;
    private final TransactionTemplate transactionTemplate;

    public PlantingKeyBackfill(VarietyRepository varietyRepository,
                               GrowthCalendarRepository growthCalendarRepository,
                               PlatformTransactionManager transactionManager) {
        this.varietyRepository = varietyRepository;
        this.growthCalendarRepository = growthCalendarRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        try {
            fill();
        } catch (RuntimeException failure) {
            log.error("The natural keys of the variety and calendar rows could not all be filled; "
                    + "the application starts anyway, and the next startup will try again.", failure);
        }
    }

    /**
     * Fills the keys of the variety rows, then of the calendar rows, oldest
     * first, so the older of two duplicates keeps the key.
     *
     * @return what was filled and which rows are duplicates
     */
    Result fill() {
        int varietiesKeyed = 0;
        List<Long> varietyDuplicates = new ArrayList<>();
        for (Variety row : varietyRepository.findByVarietyKeyIsNullOrderByIdAsc()) {
            String key = Variety.keyOf(row.getFarmId(), row.getBlockCode(), row.getName());
            if (key == null) {
                continue;
            }
            try {
                Integer updated = transactionTemplate.execute(status -> varietyRepository.fillVarietyKey(row.getId(), key));
                varietiesKeyed += updated == null ? 0 : updated;
            } catch (DataIntegrityViolationException duplicate) {
                varietyDuplicates.add(row.getId());
                log.error("Variety row {} has the same farm, block and name as row {} (key \"{}\"): its variety_key "
                                + "stays empty. Nothing was deleted.", row.getId(),
                        varietyRepository.findByVarietyKey(key).map(Variety::getId).orElse(null), key);
            }
        }

        int calendarsKeyed = 0;
        List<Long> calendarDuplicates = new ArrayList<>();
        for (GrowthCalendar row : growthCalendarRepository.findByBlockKeyIsNullOrderByIdAsc()) {
            String key = GrowthCalendar.keyOf(row.getFarmId(), row.getBlockCode());
            if (key == null) {
                continue;
            }
            try {
                Integer updated = transactionTemplate.execute(status -> growthCalendarRepository.fillBlockKey(row.getId(), key));
                calendarsKeyed += updated == null ? 0 : updated;
            } catch (DataIntegrityViolationException duplicate) {
                calendarDuplicates.add(row.getId());
                log.error("Calendar row {} has the same farm and block as row {} (key \"{}\"): its block_key stays "
                                + "empty. Nothing was deleted.", row.getId(),
                        growthCalendarRepository.findByBlockKey(key).map(GrowthCalendar::getId).orElse(null), key);
            }
        }

        if (varietiesKeyed + calendarsKeyed + varietyDuplicates.size() + calendarDuplicates.size() > 0) {
            log.info("Natural keys filled at startup: variety rows {}, calendar rows {}. Left without a key as "
                            + "duplicates: variety rows {}, calendar rows {}.", varietiesKeyed, calendarsKeyed,
                    varietyDuplicates.size(), calendarDuplicates.size());
        }
        return new Result(varietiesKeyed, varietyDuplicates, calendarsKeyed, calendarDuplicates);
    }
}
