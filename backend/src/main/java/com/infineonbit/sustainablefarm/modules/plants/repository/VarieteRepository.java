package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VarieteRepository extends JpaRepository<Variete, Long> {

    /**
     * Returns the varieties matching the given filters.
     *
     * <p>Both parameters are optional: a {@code null} parameter disables its own
     * filter, so the same query serves "all varieties", "one farm", "one block"
     * and "one block of one farm". A filter that matches nothing yields an empty
     * list, never an error.
     *
     * @param idFerme      farm identifier, or {@code null} to ignore the farm
     * @param blocParcelle raw block value as stored (for example {@code "A"}),
     *                     or {@code null} to ignore the block
     * @return the matching varieties, ordered by block then name
     */
    @Query("""
            SELECT v FROM Variete v
            WHERE (:idFerme IS NULL OR v.idFerme = :idFerme)
              AND (:blocParcelle IS NULL OR v.blocParcelle = :blocParcelle)
            ORDER BY v.blocParcelle ASC, v.nom ASC
            """)
    List<Variete> findByOptionalFilters(@Param("idFerme") Integer idFerme,
                                        @Param("blocParcelle") String blocParcelle);
}
