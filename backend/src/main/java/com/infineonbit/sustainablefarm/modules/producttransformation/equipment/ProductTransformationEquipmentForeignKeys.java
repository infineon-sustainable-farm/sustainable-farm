package com.infineonbit.sustainablefarm.modules.producttransformation.equipment;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey.OnDelete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from producttransformation to the machinery equipment register (ADR 0004),
 * added by {@code core.schema.CrossModuleForeignKeyInstaller}.
 *
 * <ul>
 *   <li>{@code pt_equipment_spec}, the 1:1 extension, goes with its equipment (cascade).</li>
 *   <li>The drying, wash and sort, and packaging records refuse the deletion of a machine they
 *       still use, as product transformation did before the merge.</li>
 * </ul>
 */
@Configuration
public class ProductTransformationEquipmentForeignKeys {

    static final String TABLE = "equipment";
    static final String KEY_COLUMN = "id";

    @Bean
    CrossModuleForeignKey equipmentSpecEquipmentForeignKey() {
        return new CrossModuleForeignKey("fk_pt_equipment_spec_equipment", "pt_equipment_spec", "equipment_id",
                TABLE, KEY_COLUMN, OnDelete.CASCADE);
    }

    @Bean
    CrossModuleForeignKey dryingRunEquipmentForeignKey() {
        return toRegister("fk_drying_run_equipment", "drying_run");
    }

    @Bean
    CrossModuleForeignKey washSortRecordEquipmentForeignKey() {
        return toRegister("fk_wash_sort_record_equipment", "wash_sort_record");
    }

    @Bean
    CrossModuleForeignKey packagingRecordEquipmentForeignKey() {
        return toRegister("fk_packaging_record_equipment", "packaging_record");
    }

    private static CrossModuleForeignKey toRegister(String name, String table) {
        return new CrossModuleForeignKey(name, table, "equipment_id", TABLE, KEY_COLUMN);
    }
}
