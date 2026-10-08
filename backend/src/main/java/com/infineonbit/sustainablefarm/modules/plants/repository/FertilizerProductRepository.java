package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FertilizerProductRepository extends JpaRepository<FertilizerProduct, Long> {

    /** The whole catalogue, ordered by name. */
    List<FertilizerProduct> findAllByOrderByNameAsc();
}
