package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.impl;

import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.MaintenanceStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * How product transformation equipment maps onto the farm equipment register of machinery.
 *
 * <ul>
 *   <li>Product transformation lists the register equipment of the stages
 *       {@link Stage#PROCESSING} and {@link Stage#DRYING}, whatever their category, and
 *       registers its own with the category {@code PROCESSING_MACHINERY}.</li>
 *   <li>Stage of a type: the dryers ({@code DRYING}, {@code TESTA_DRYER}, {@code SOLAR_DRYER},
 *       {@code TUNNEL_DRYER}) serve {@link Stage#DRYING}; {@code WASHING} and
 *       {@code PACKAGING} serve {@link Stage#PROCESSING}.</li>
 *   <li>Status: one status per machine, the register's. {@code ACTIVE} is
 *       {@link Status#OPERATIONAL}, {@code MAINTENANCE} is {@link Status#UNDER_MAINTENANCE},
 *       {@code RETIRED} is {@link Status#OUT_OF_SERVICE}.</li>
 *   <li>Codes and names are unique in the whole register: creating or renaming product
 *       transformation equipment with a code or a name already used, by machinery equipment
 *       too, answers 409 (a create with an existing code used to overwrite it silently, and two
 *       product transformation machines could share a name).</li>
 *   <li>A machine still used by a drying run, a wash and sort record or a packaging record
 *       cannot be deleted, from either module: 409, as product transformation answered
 *       before.</li>
 * </ul>
 */
public final class EquipmentRegisterMapping {

    /** The register stages product transformation works with. */
    public static final Set<Stage> PROCESSING_STAGES = EnumSet.of(Stage.PROCESSING, Stage.DRYING);

    private EquipmentRegisterMapping() {
    }

    public static Stage stageOf(EquipmentType type) {
        return switch (type) {
            case WASHING, PACKAGING -> Stage.PROCESSING;
            case DRYING, TESTA_DRYER, SOLAR_DRYER, TUNNEL_DRYER -> Stage.DRYING;
        };
    }

    public static Status statusOf(MaintenanceStatus maintenanceStatus) {
        return switch (maintenanceStatus) {
            case ACTIVE -> Status.OPERATIONAL;
            case MAINTENANCE -> Status.UNDER_MAINTENANCE;
            case RETIRED -> Status.OUT_OF_SERVICE;
        };
    }

    public static MaintenanceStatus maintenanceStatusOf(Status status) {
        return switch (status) {
            case OPERATIONAL -> MaintenanceStatus.ACTIVE;
            case UNDER_MAINTENANCE -> MaintenanceStatus.MAINTENANCE;
            case OUT_OF_SERVICE -> MaintenanceStatus.RETIRED;
        };
    }
}
