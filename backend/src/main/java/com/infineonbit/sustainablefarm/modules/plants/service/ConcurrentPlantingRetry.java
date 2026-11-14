package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Runs a planting or harvest write a second time when it lost a race on the
 * natural key of a variety row or of a calendar row, or on the lot number of a
 * harvest.
 *
 * <p>Two plantings of a new triple, or of a new block, sent at the same time
 * both find no row and both insert one. Likewise, two harvests of a block sent
 * at the same time both read the same highest lot number and both take the
 * next one. The unique constraint makes the second insert wait for the first
 * transaction, then refuses it once the first one has committed. The losing
 * transaction cannot go on: PostgreSQL aborts it, and Hibernate cannot query
 * again after the failed insert. The write is therefore run again, once, in a
 * new transaction. It now sees the rows of the first request and ends like a
 * write sent after it: a planting on the existing 409 when the variety is
 * planted, or on a 201; a harvest on a 201 with the next lot number.
 *
 * <p>Must be called outside any transaction, as the controllers do; the
 * transplant of a nursery batch is run again as a whole and stays
 * all-or-nothing. Only the three key constraints are handled: any other
 * integrity violation goes through unchanged and is never run again. When the
 * second run loses a race again, for example with three requests or more on
 * the same block, the request is refused with a 409 and nothing is saved.
 */
@Component
public class ConcurrentPlantingRetry {

    /** The unique constraints on the natural keys; a violation of any other one is not a lost race. */
    private static final List<String> KEY_CONSTRAINTS = List.of(Variety.KEY_CONSTRAINT, GrowthCalendar.KEY_CONSTRAINT,
            HarvestRecord.LOT_NUMBER_CONSTRAINT);

    /**
     * The key constraint the violation comes from, if it is one of the three.
     *
     * <p>The constraint name is read as each database reports it:
     * {@code uk_varietes_variety_key} on PostgreSQL,
     * {@code PUBLIC.UK_VARIETES_VARIETY_KEY INDEX ...} on H2. Only its first
     * word is compared, without the schema and ignoring case, so the rest of
     * the message, which can quote the name of a variety, is never read.
     *
     * @param violation the violation raised by the write
     * @return the name of the key constraint in lower case, or {@code null} for
     *         a violation of any other constraint
     */
    static String keyConstraintOf(DataIntegrityViolationException violation) {
        for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                String name = constraintViolation.getConstraintName().trim().split("\\s+")[0];
                name = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                return KEY_CONSTRAINTS.contains(name) ? name : null;
            }
        }
        return null;
    }

    /**
     * Runs the write, and runs it once more when it lost a race on a key
     * constraint.
     *
     * @param blockCode the block code of the request, as received
     * @param write     the write, which opens its own transaction
     * @param <T>       the result of the write
     * @return the result of the first run, or of the second one after a lost race
     * @throws ConflictException                if the second run loses a race again
     * @throws DataIntegrityViolationException  if a constraint other than the three keys is violated
     */
    public <T> T runRetryingOnce(String blockCode, Supplier<T> write) {
        try {
            return write.get();
        } catch (DataIntegrityViolationException lostRace) {
            if (keyConstraintOf(lostRace) == null) {
                throw lostRace;
            }
        }
        try {
            return write.get();
        } catch (DataIntegrityViolationException lostAgain) {
            String constraint = keyConstraintOf(lostAgain);
            if (constraint == null) {
                throw lostAgain;
            }
            String kind = HarvestRecord.LOT_NUMBER_CONSTRAINT.equals(constraint) ? "harvest" : "planting";
            throw new ConflictException("Another " + kind + " on block " + PlantingService.normalizeBlockCode(blockCode)
                    + " was being recorded at the same time. Nothing was saved: please send the request again.");
        }
    }
}
