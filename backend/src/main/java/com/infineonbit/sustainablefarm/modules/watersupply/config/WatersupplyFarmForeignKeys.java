package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from the farm and zone columns of Water Supply to the farm referential,
 * added by {@code core.schema.CrossModuleForeignKeyInstaller} (docs/adr/0004).
 */
@Configuration
public class WatersupplyFarmForeignKeys {

    @Bean
    CrossModuleForeignKey waterSourceFarmForeignKey() {
        return toFarm("fk_water_sources_farm", "water_sources");
    }

    @Bean
    CrossModuleForeignKey waterConsumptionFarmForeignKey() {
        return toFarm("fk_water_consumption_farm", "water_consumption");
    }

    @Bean
    CrossModuleForeignKey waterConsumptionZoneForeignKey() {
        return toZone("fk_water_consumption_zone", "water_consumption");
    }

    @Bean
    CrossModuleForeignKey irrigationScheduleZoneForeignKey() {
        return toZone("fk_irrigation_schedules_zone", "irrigation_schedules");
    }

    @Bean
    CrossModuleForeignKey soilMoistureReadingZoneForeignKey() {
        return toZone("fk_soil_moisture_readings_zone", "soil_moisture_readings");
    }

    @Bean
    CrossModuleForeignKey dripMaintenanceLogZoneForeignKey() {
        return toZone("fk_drip_maintenance_logs_zone", "drip_maintenance_logs");
    }

    private static CrossModuleForeignKey toFarm(String name, String table) {
        return new CrossModuleForeignKey(name, table, "farm_id", FarmDirectory.FARM_TABLE, FarmDirectory.KEY_COLUMN);
    }

    private static CrossModuleForeignKey toZone(String name, String table) {
        return new CrossModuleForeignKey(name, table, "zone_id", FarmDirectory.ZONE_TABLE, FarmDirectory.KEY_COLUMN);
    }
}
