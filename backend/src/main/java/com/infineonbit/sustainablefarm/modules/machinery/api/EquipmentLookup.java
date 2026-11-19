package com.infineonbit.sustainablefarm.modules.machinery.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read access to the farm equipment register owned by machinery (ADR 0004).
 *
 * <p>Machinery is the only module that creates, renames, changes the status of and deletes a
 * machine, whatever its stage; the other modules pick one by its id ({@link EquipmentView#id()}),
 * with a foreign key to {@code equipment(id)}, and never write the register.</p>
 */
public interface EquipmentLookup {

    /**
     * Order of the {@code dev} seeder of the demo machines. A seeder of another module that
     * references them (product transformation) runs after it (a higher {@code @Order} value).
     */
    int DEMO_SEED_ORDER = -50;

    /** The equipment with this register id, if any. */
    Optional<EquipmentView> findById(Long id);

    /** The equipment with this business code, if any. */
    Optional<EquipmentView> findByCode(String code);

    /** Every piece of equipment that serves one of these stages, ordered by id. */
    List<EquipmentView> findByStages(Collection<Stage> stages);
}
