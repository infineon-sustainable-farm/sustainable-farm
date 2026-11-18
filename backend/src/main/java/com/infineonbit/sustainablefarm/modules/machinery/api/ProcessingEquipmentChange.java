package com.infineonbit.sustainablefarm.modules.machinery.api;

/**
 * A change to a processing or drying machine of the register; a {@code null} field is left
 * unchanged.
 *
 * @param name   the new unique name
 * @param stage  the new stage, {@link Stage#PROCESSING} or {@link Stage#DRYING}
 * @param status the new status
 */
public record ProcessingEquipmentChange(String name, Stage stage, Status status) {
}
