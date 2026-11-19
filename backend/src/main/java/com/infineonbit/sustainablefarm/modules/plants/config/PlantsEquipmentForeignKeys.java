package com.infineonbit.sustainablefarm.modules.plants.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey.OnDelete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign key from the treatments of plants to the machinery equipment register, checked or
 * added by {@code core.schema.CrossModuleForeignKeyInstaller} (docs/adr/0004). Machinery owns
 * the machines; a treatment keeps the name of its machine, so deleting the machine in machinery
 * only empties {@code equipment_id} ({@code on delete set null}) and the treatment record stays
 * complete.
 */
@Configuration
public class PlantsEquipmentForeignKeys {

    @Bean
    CrossModuleForeignKey healthTreatmentEquipmentForeignKey() {
        return new CrossModuleForeignKey("fk_health_treatment_equipment", "health_treatment", "equipment_id",
                "equipment", "id", OnDelete.SET_NULL);
    }
}
