package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.impl;

import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * How product transformation equipment maps onto the farm equipment register of machinery.
 *
 * <ul>
 *   <li>Product transformation lists the register equipment of the stages
 *       {@link Stage#PROCESSING} and {@link Stage#DRYING}, whatever their category. Machinery
 *       alone creates, renames, changes the status of and deletes them; product transformation
 *       writes only their attributes ({@code pt_equipment_spec}).</li>
 *   <li>Status: one status per machine, the register's, read in product transformation terms.
 *       {@link Status#OPERATIONAL} is {@code ACTIVE}, {@link Status#UNDER_MAINTENANCE} is
 *       {@code MAINTENANCE}, {@link Status#OUT_OF_SERVICE} is {@code RETIRED}.</li>
 *   <li>A machine still used by a drying run, a wash and sort record or a packaging record
 *       cannot be deleted in machinery: 409 (foreign keys of
 *       {@code ProductTransformationEquipmentForeignKeys}).</li>
 * </ul>
 */
public final class EquipmentRegisterMapping {

    /** The register stages product transformation works with. */
    public static final Set<Stage> PROCESSING_STAGES = EnumSet.of(Stage.PROCESSING, Stage.DRYING);

    private EquipmentRegisterMapping() {
    }

    public static MaintenanceStatus maintenanceStatusOf(Status status) {
        return switch (status) {
            case OPERATIONAL -> MaintenanceStatus.ACTIVE;
            case UNDER_MAINTENANCE -> MaintenanceStatus.MAINTENANCE;
            case OUT_OF_SERVICE -> MaintenanceStatus.RETIRED;
        };
    }
}
