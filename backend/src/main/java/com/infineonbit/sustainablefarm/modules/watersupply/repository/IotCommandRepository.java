package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IotCommandRepository extends JpaRepository<IotCommand, UUID> {
    /** Command journal of a device, most recent first (audit). */
    List<IotCommand> findByDeviceIdOrderByRequestedAtDesc(String deviceId);
}
