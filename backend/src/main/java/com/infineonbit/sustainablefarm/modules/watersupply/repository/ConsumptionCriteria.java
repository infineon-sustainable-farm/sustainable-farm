package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Optional filters on {@code water_consumption}; a {@code null} criterion is not applied.
 *
 * <p>Built as a Criteria predicate rather than a JPQL {@code (:p is null or ...)}: PostgreSQL cannot
 * infer the type of a parameter that only appears in {@code ? IS NULL}, so such a query passes on
 * H2 and fails in the CI (same issue as documented in the plants repositories).</p>
 *
 * @param zoneIds zones allowed (block filter); an empty collection matches nothing
 * @param from    inclusive lower bound of {@code consumptionDate}
 * @param to      exclusive upper bound of {@code consumptionDate}
 */
public record ConsumptionCriteria(UUID farmId, UUID sourceId, UUID zoneId, Collection<UUID> zoneIds,
                                  String lotId, Instant from, Instant to) {

    public static ConsumptionCriteria window(UUID sourceId, UUID zoneId, Instant from, Instant to) {
        return new ConsumptionCriteria(null, sourceId, zoneId, null, null, from, to);
    }

    public Specification<WaterConsumption> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (farmId != null) {
                predicates.add(cb.equal(root.get("farmId"), farmId));
            }
            if (sourceId != null) {
                predicates.add(cb.equal(root.get("sourceId"), sourceId));
            }
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zoneId"), zoneId));
            }
            if (zoneIds != null) {
                predicates.add(zoneIds.isEmpty() ? cb.disjunction() : root.get("zoneId").in(zoneIds));
            }
            if (lotId != null) {
                predicates.add(cb.equal(root.get("lotId"), lotId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("consumptionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<Instant>get("consumptionDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
