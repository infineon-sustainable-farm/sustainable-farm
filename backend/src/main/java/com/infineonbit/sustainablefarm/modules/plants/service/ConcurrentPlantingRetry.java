package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Runs a planting write a second time when it lost a race on the natural key
 * of a variety row or of a calendar row.
 *
 * <p>Two plantings of a new triple, or of a new block, sent at the same time
 * both find no row and both insert one. The unique constraint makes the second
 * insert wait for the first transaction, then refuses it once the first one has
 * committed. The losing transaction cannot go on: PostgreSQL aborts it, and
 * Hibernate cannot query again after the failed insert. The write is therefore
 * run again, once, in a new transaction. It now finds the rows of the first
 * request and ends like a planting sent after it: on the existing 409 when the
 * variety is planted, or on a 201.
 *
 * <p>Must be called outside any transaction, as the controllers do; the
 * transplant of a nursery batch is run again as a whole and stays
 * all-or-nothing. Only the two key constraints are handled: any other integrity
 * violation goes through unchanged and is never run again. When the second run
 * loses a race again, for example with three requests or more on a new block,
 * the request is refused with a 409 and nothing is saved.
 */
@Component
public class ConcurrentPlantingRetry {

    /** The unique constraints on the natural keys; a violation of any other one is not a lost race. */
    private static final List<String> KEY_CONSTRAINTS = List.of(Variety.KEY_CONSTRAINT, GrowthCalendar.KEY_CONSTRAINT);

    /**
     * Whether the violation comes from one of the two key constraints.
     *
     * <p>The constraint name is read as each database reports it:
     * {@code uk_varietes_variety_key} on PostgreSQL,
     * {@code PUBLIC.UK_VARIETES_VARIETY_KEY INDEX ...} on H2. Only its first
     * word is compared, without the schema and ignoring case, so the rest of
     * the message, which can quote the name of a variety, is never read.
     *
     * @param violation the violation raised by the write
     * @return {@code true} for a violation of one of the key constraints
     */
    static boolean isKeyViolation(DataIntegrityViolationException violation) {
        for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                String name = constraintViolation.getConstraintName().trim().split("\\s+")[0];
                name = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                return KEY_CONSTRAINTS.contains(name);
            }
        }
        return false;
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
     * @throws DataIntegrityViolationException  if a constraint other than the two keys is violated
     */
    public <T> T runRetryingOnce(String blockCode, Supplier<T> write) {
        try {
            return write.get();
        } catch (DataIntegrityViolationException lostRace) {
            if (!isKeyViolation(lostRace)) {
                throw lostRace;
            }
        }
        try {
            return write.get();
        } catch (DataIntegrityViolationException lostAgain) {
            if (!isKeyViolation(lostAgain)) {
                throw lostAgain;
            }
            throw new ConflictException("Another planting on block " + PlantingService.normalizeBlockCode(blockCode)
                    + " was being recorded at the same time. Nothing was saved: please send the request again.");
        }
    }
}
