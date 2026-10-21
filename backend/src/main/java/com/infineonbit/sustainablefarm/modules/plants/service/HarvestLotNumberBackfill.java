package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
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
 * Gives, at startup in the dev profile, a lot number to the harvests recorded
 * before lot numbers existed.
 *
 * <p>The harvests are taken oldest first, and each one gets the next number of
 * its block for the year of its harvest date, as a harvest recorded now would:
 * the numbers follow the order in which they are given, so a harvest numbered
 * here comes after the lots already given to its block and year. Each harvest
 * is filled in its own transaction, with a bulk update that changes no other
 * column: {@code last_updated} keeps its value.
 *
 * <p>Runs in the dev profile only, like {@link PlantingKeyBackfill}. In another
 * profile, the harvests recorded before lot numbers existed keep no lot number.
 *
 * <p>It never stops the startup. A harvest whose number is taken at the same
 * moment by a harvest being recorded keeps no lot number until the next
 * startup; any other failure is logged.
 */
@Component
@Profile("dev")
public class HarvestLotNumberBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HarvestLotNumberBackfill.class);

    /**
     * What one filling did.
     *
     * @param numbered   number of harvests given a lot number
     * @param leftAsIs   harvests left without a lot number, their number having been taken at the same moment
     */
    record Result(int numbered, List<Long> leftAsIs) {
    }

    private final HarvestRecordRepository harvestRecordRepository;
    private final HarvestService harvestService;
    private final TransactionTemplate transactionTemplate;

    public HarvestLotNumberBackfill(HarvestRecordRepository harvestRecordRepository, HarvestService harvestService,
                                    PlatformTransactionManager transactionManager) {
        this.harvestRecordRepository = harvestRecordRepository;
        this.harvestService = harvestService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        try {
            fill();
        } catch (RuntimeException failure) {
            log.error("The lot numbers of the earlier harvests could not all be given; the application starts "
                    + "anyway, and the next startup will try again.", failure);
        }
    }

    /**
     * Gives a lot number to every harvest without one, oldest first.
     *
     * @return how many harvests were numbered, and which ones were left as they were
     */
    Result fill() {
        int numbered = 0;
        List<Long> leftAsIs = new ArrayList<>();
        for (HarvestRecord harvest : harvestRecordRepository.findWithoutLotNumberOldestFirst()) {
            String blockCode = harvest.getVariety().getBlockCode();
            int year = harvest.getHarvestDate().getYear();
            try {
                Integer updated = transactionTemplate.execute(status -> harvestRecordRepository.fillLotNumber(
                        harvest.getId(), harvestService.nextLotNumber(blockCode, year)));
                numbered += updated == null ? 0 : updated;
            } catch (DataIntegrityViolationException taken) {
                leftAsIs.add(harvest.getId());
                log.error("Harvest {} got no lot number: the next number of block {} for {} was taken at the same "
                        + "moment. It stays without one until the next startup.", harvest.getId(), blockCode, year);
            }
        }
        if (numbered + leftAsIs.size() > 0) {
            log.info("Lot numbers given at startup to earlier harvests: {}. Left without one: {}.",
                    numbered, leftAsIs.size());
        }
        return new Result(numbered, leftAsIs);
    }
}
