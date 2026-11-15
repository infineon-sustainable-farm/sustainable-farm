package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WaterQualityTestRepository extends JpaRepository<WaterQualityTest, UUID> {
	Page<WaterQualityTest> findBySourceId(UUID sourceId, Pageable pageable);

	/** Tests of a closed window [from, to], all sources (quality compliance without lot). */
	@Query("select t from WaterQualityTest t where t.testDate >= :from and t.testDate <= :to order by t.testDate asc")
	List<WaterQualityTest> findBetween(Instant from, Instant to);

	/** Tests of the given sources over a closed window [from, to] (callers pass a non-empty list). */
	@Query("select t from WaterQualityTest t where t.sourceId in :sourceIds "
			+ "and t.testDate >= :from and t.testDate <= :to order by t.testDate asc")
	List<WaterQualityTest> findBySourcesBetween(Collection<UUID> sourceIds, Instant from, Instant to);
}
