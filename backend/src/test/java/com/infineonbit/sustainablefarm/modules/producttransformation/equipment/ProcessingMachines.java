package com.infineonbit.sustainablefarm.modules.producttransformation.equipment;

import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Request.EquipmentCreationRequest;
import com.infineonbit.sustainablefarm.modules.machinery.dto.Response.EquipmentObtainingResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.Equipment;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentType;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.service.EquipmentService;

import java.math.BigDecimal;

/**
 * Test data: a processing or drying machine as the application makes one. Machinery registers the
 * machine (the only module that creates equipment), then product transformation adds its
 * attributes.
 */
public final class ProcessingMachines {

    private ProcessingMachines() {
    }

    public static Equipment register(com.infineonbit.sustainablefarm.modules.machinery.service.EquipmentService machinery,
                                     EquipmentService productTransformation,
                                     String name, Stage stage, EquipmentType type,
                                     String capacityKgPerHour, String energyKwhPerKg) {
        EquipmentObtainingResponse machine = machinery.addEquipment(
                new EquipmentCreationRequest(name, Category.PROCESSING_MACHINERY, stage, Status.OPERATIONAL));
        Equipment attributes = new Equipment();
        attributes.setEquipmentType(type);
        attributes.setCapacityKgPerHour(new BigDecimal(capacityKgPerHour));
        attributes.setEnergyConsumptionKwhPerKg(new BigDecimal(energyKwhPerKg));
        return productTransformation.updateSpec(machine.code(), attributes);
    }
}
