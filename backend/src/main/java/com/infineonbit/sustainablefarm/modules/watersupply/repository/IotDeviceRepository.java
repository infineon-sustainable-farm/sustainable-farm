package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotDevice;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    IotDevice findByDeviceUid(String deviceUid);

    @Query("select count(d) from IotDevice d")
    long countAll();

    @Query("select count(d) from IotDevice d where d.lastSeen is not null")
    long countSeen();

    @Query("select d from IotDevice d where d.lastSeen is null or d.lastSeen < :cutoff order by d.lastSeen asc")
    List<IotDevice> findOfflineSince(Instant cutoff);
}
