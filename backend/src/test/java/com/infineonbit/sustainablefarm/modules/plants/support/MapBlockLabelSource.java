package com.infineonbit.sustainablefarm.modules.plants.support;

import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabel;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabelSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * The block labels of a {@code @DataJpaTest}, which has no farm referential: the test puts the
 * label of each block id it uses, and the plants rows it loads read them. Import
 * {@link Config} to give the bean to the entity listener.
 */
public class MapBlockLabelSource implements BlockLabelSource {

    private final Map<Long, BlockLabel> labels = new ConcurrentHashMap<>();

    /** Gives a block id its label. */
    public MapBlockLabelSource put(Long blockId, BlockLabel label) {
        labels.put(blockId, label);
        return this;
    }

    @Override
    public BlockLabel labelOf(Long blockId) {
        return labels.getOrDefault(blockId, BlockLabel.NONE);
    }

    /** The bean of a {@code @DataJpaTest}. */
    @TestConfiguration
    public static class Config {
        @Bean
        MapBlockLabelSource mapBlockLabelSource() {
            return new MapBlockLabelSource();
        }
    }
}
