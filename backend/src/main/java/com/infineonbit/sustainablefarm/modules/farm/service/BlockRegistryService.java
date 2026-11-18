package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link BlockRegistry} implementation: the block of a code, created under its farm when it is new.
 *
 * <p>A new block is written in a transaction of its own, so that two first writes on the same new
 * code sent at the same time both succeed: the one that loses the race on {@link #CODE_CONSTRAINT}
 * reads the block of the winner instead of failing its caller's transaction. When that separate
 * transaction cannot see the farm (a farm created by the caller and not committed yet), the block is
 * created in the caller's transaction.</p>
 */
@Service
@Transactional
public class BlockRegistryService implements BlockRegistry {
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final TransactionTemplate newTransaction;

    public BlockRegistryService(FarmRepository farmRepository, BlockRepository blockRepository,
                                PlatformTransactionManager transactionManager) {
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public BlockDto findOrCreateBlock(Long farmId, String code) {
        String normalized = FarmDirectory.normalizeBlockCode(code);
        if (normalized == null) {
            throw new BadRequestException("blockCode must not be blank");
        }
        Optional<BlockDto> existing = blockRepository.findByCode(normalized).map(FarmMapper::toDto);
        if (existing.isEmpty()) {
            try {
                existing = Optional.ofNullable(newTransaction.execute(status -> create(farmId, normalized)));
            } catch (DataIntegrityViolationException | BadRequestException lostRaceOrUncommittedFarm) {
                // Lost race: the block of the winner is committed. Farm not visible to the separate
                // transaction: no block, the creation below runs in the caller's transaction.
                existing = blockRepository.findByCode(normalized).map(FarmMapper::toDto);
            }
        }
        BlockDto block = existing.orElseGet(() -> create(farmId, normalized));
        if (farmId != null && !farmId.equals(block.farmId())) {
            throw new BadRequestException("Block " + normalized + " belongs to farm " + block.farmId()
                    + ", not to farm " + farmId);
        }
        return block;
    }

    private BlockDto create(Long farmId, String normalized) {
        Block block = new Block();
        block.setFarm(farmId == null ? defaultFarm() : farmRepository.findById(farmId)
                .orElseThrow(() -> new BadRequestException("Farm " + farmId + " not found")));
        block.setCode(normalized);
        block.setName(normalized);
        block.setAreaHectares(0.0);
        return FarmMapper.toDto(blockRepository.saveAndFlush(block));
    }

    private Farm defaultFarm() {
        return farmRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            Farm farm = new Farm();
            farm.setName(DEFAULT_FARM_NAME);
            return farmRepository.save(farm);
        });
    }
}
