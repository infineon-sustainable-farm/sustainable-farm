package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model;

/**
 * Maintenance Status Enum, the register status in product transformation terms
 * (see {@code EquipmentRegisterMapping}).
 * Business Rule: Equipment must be ACTIVE for assignment
 */
public enum MaintenanceStatus {
    ACTIVE,
    MAINTENANCE,
    RETIRED
}
