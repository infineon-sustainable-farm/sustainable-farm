package com.infineonbit.sustainablefarm.modules.producttransformation.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.Table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards Option A: Product Transformation and Machinery own two distinct equipment
 * tables and must never share one.
 *
 * <p>The two modules both model a machine registry, with different lifecycles and
 * different owners. Sharing the {@code equipment} table would let one module read and
 * write the other's rows, silently. The guard pins the physical table name of each
 * entity so a rename, a copy-paste or a merge that drops the annotation fails here
 * instead of in production.
 *
 * <p>Plain reflection, no Spring context: the test is a contract on the mapping and
 * stays fast.
 *
 * @author audit Product Transformation (SABF226)
 */
@DisplayName("Equipment ownership boundary (Option A)")
class EquipmentOwnershipBoundaryTest {

    private static final Class<?> PRODUCT_TRANSFORMATION_EQUIPMENT =
            com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment.class;

    private static final Class<?> MACHINERY_EQUIPMENT =
            com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment.class;

    private static String tableNameOf(Class<?> entity) {
        Table table = entity.getAnnotation(Table.class);
        assertNotNull(table, entity.getName() + " must declare @Table");
        return table.name();
    }

    @Test
    @DisplayName("Product Transformation owns pt_equipment")
    void productTransformationDeclaresItsOwnEquipmentTable() {
        assertEquals("pt_equipment", tableNameOf(PRODUCT_TRANSFORMATION_EQUIPMENT));
    }

    @Test
    @DisplayName("Machinery keeps equipment")
    void machineryKeepsItsOwnEquipmentTable() {
        assertEquals("equipment", tableNameOf(MACHINERY_EQUIPMENT));
    }

    @Test
    @DisplayName("The two equipment tables are distinct")
    void theTwoEquipmentTablesMustDiffer() {
        assertNotEquals(
                tableNameOf(MACHINERY_EQUIPMENT),
                tableNameOf(PRODUCT_TRANSFORMATION_EQUIPMENT),
                "Product Transformation and Machinery must not share an equipment table (Option A)");
    }
}
