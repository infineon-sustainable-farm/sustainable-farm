package com.infineonbit.sustainablefarm.modules.farm.support;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test data of the site referential for the integration tests of the modules that pick a block
 * (Plants, Product Transformation, Water Supply): the farm module owns the blocks, so a test adds
 * the blocks its requests designate before sending them, as the Farms &amp; fields screen would.
 */
@Component
@Profile("test")
public class SiteTestBlocks {

    /** Name of the farm of the blocks added without a farm. */
    public static final String TEST_FARM_NAME = "Test farm";

    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final FarmDirectory farmDirectory;

    public SiteTestBlocks(FarmRepository farmRepository, BlockRepository blockRepository, FarmDirectory farmDirectory) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.farmDirectory = farmDirectory;
    }

    /** The block of a code, added under the test farm when no block has it. */
    @Transactional
    public BlockDto ensure(String code) {
        return ensure(null, code);
    }

    /** The block of a code, added under the farm (the test farm for {@code null}) when no block has it. */
    @Transactional
    public BlockDto ensure(Long farmId, String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        Long blockId = blockRepository.findByCode(normalized).map(Block::getId).orElseGet(() -> {
            Block block = new Block();
            block.setFarm(farmId == null ? testFarm() : farmRepository.findById(farmId).orElseThrow());
            block.setCode(normalized);
            block.setName(normalized);
            block.setAreaHectares(0.0);
            return blockRepository.saveAndFlush(block).getId();
        });
        return farmDirectory.findBlock(blockId).orElseThrow();
    }

    /** Adds the blocks of several codes under the test farm. */
    public void ensureAll(String... codes) {
        for (String code : codes) {
            ensure(code);
        }
    }

    /** A new farm, for the blocks of a test that must not share a farm. */
    @Transactional
    public Long newFarm(String name) {
        Farm farm = new Farm();
        farm.setName(name);
        return farmRepository.saveAndFlush(farm).getId();
    }

    private Farm testFarm() {
        return farmRepository.findFirstByNameIgnoreCaseOrderByIdAsc(TEST_FARM_NAME).orElseGet(() -> {
            Farm farm = new Farm();
            farm.setName(TEST_FARM_NAME);
            return farmRepository.saveAndFlush(farm);
        });
    }
}
