package com.infineonbit.sustainablefarm.modules.farm.service;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Whether rows of the other modules reference a block, read from the foreign keys they declare
 * to {@link FarmDirectory#BLOCK_TABLE} ({@link CrossModuleForeignKey}).
 *
 * <p>Plants and Product Transformation designate a block by its code: a referenced block keeps its
 * code, otherwise their rows would lose the block they show.</p>
 */
@Component
class BlockReferences {
    private final JdbcTemplate jdbc;
    private final List<CrossModuleForeignKey> blockForeignKeys;

    BlockReferences(JdbcTemplate jdbc, ObjectProvider<CrossModuleForeignKey> foreignKeys) {
        this.jdbc = jdbc;
        this.blockForeignKeys = foreignKeys.orderedStream()
                .filter(key -> FarmDirectory.BLOCK_TABLE.equalsIgnoreCase(key.referencedTable()))
                .toList();
    }

    boolean isReferenced(Long blockId) {
        for (CrossModuleForeignKey key : blockForeignKeys) {
            List<Integer> found = jdbc.queryForList(
                    "SELECT 1 FROM " + key.table() + " WHERE " + key.column() + " = ? FETCH FIRST 1 ROWS ONLY",
                    Integer.class, blockId);
            if (!found.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
