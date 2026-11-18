package com.infineonbit.sustainablefarm.modules.machinery.api;

/**
 * Write access to the processing and drying machines of the farm equipment register, for the
 * modules that manage them from their own screens (product transformation).
 *
 * <p>Only equipment of the stages {@link Stage#PROCESSING} and {@link Stage#DRYING} can be
 * changed or deleted here; the rest of the register is managed by machinery alone.</p>
 */
public interface ProcessingEquipmentRegistry {

    /**
     * Registers a processing machine.
     *
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException if the code or
     *         the name is already used
     * @throws com.infineonbit.sustainablefarm.core.exception.BadRequestException if the stage is
     *         not a processing stage or a field is missing or too long
     */
    EquipmentView register(ProcessingEquipmentRegistration registration);

    /**
     * Changes a processing or drying machine.
     *
     * @throws com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException if no
     *         processing or drying machine has this id
     */
    EquipmentView change(Long id, ProcessingEquipmentChange change);

    /**
     * Deletes a processing or drying machine; machinery logs are kept and detached, like a
     * deletion from the machinery screens.
     *
     * @throws com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException if no
     *         processing or drying machine has this id
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException if records of
     *         another module still reference it
     */
    void delete(Long id);
}
