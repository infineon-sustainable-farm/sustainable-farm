package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.CalendrierCroissance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendrierCroissanceRepository extends JpaRepository<CalendrierCroissance, Long> {

    /**
     * Returns the growth calendar entries matching the given filters.
     *
     * <p>Both parameters are optional: a {@code null} parameter disables its own
     * filter. A filter that matches nothing yields an empty list, never an error.
     *
     * @param idFerme      farm identifier, or {@code null} to ignore the farm
     * @param blocParcelle raw block value as stored (for example {@code "A"}),
     *                     or {@code null} to ignore the block
     * @return the matching entries, ordered by block then planting date
     */
    @Query("""
            SELECT c FROM CalendrierCroissance c
            WHERE (:idFerme IS NULL OR c.idFerme = :idFerme)
              AND (:blocParcelle IS NULL OR c.blocParcelle = :blocParcelle)
            ORDER BY c.blocParcelle ASC, c.datePlantation ASC NULLS LAST, c.id ASC
            """)
    List<CalendrierCroissance> findByOptionalFilters(@Param("idFerme") Integer idFerme,
                                                     @Param("blocParcelle") String blocParcelle);
}
