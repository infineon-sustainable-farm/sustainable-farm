package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class GrowthCalendarRepositoryTest {

    @Autowired
    private GrowthCalendarRepository growthCalendarRepository;

    private GrowthCalendar save(Integer farmId, String blockCode) {
        GrowthCalendar entry = new GrowthCalendar();
        entry.setFarmId(farmId);
        entry.setBlockCode(blockCode);
        return growthCalendarRepository.save(entry);
    }

    private static List<Long> ids(List<GrowthCalendar> entries) {
        return entries.stream().map(GrowthCalendar::getId).toList();
    }

    @Test
    void findByFarmAndBlock_shouldKeepFarmNullAndFarm1Apart() {
        // Arrange: block "A" of no farm and block "A" of farm 1 are two different blocks
        GrowthCalendar noFarm = save(null, "A");
        GrowthCalendar farm1 = save(1, "A");
        // Act
        List<GrowthCalendar> foundWithoutFarm = growthCalendarRepository.findByFarmAndBlock(null, "A");
        List<GrowthCalendar> foundForFarm1 = growthCalendarRepository.findByFarmAndBlock(1, "A");
        // Assert
        assertEquals(List.of(noFarm.getId()), ids(foundWithoutFarm));
        assertEquals(List.of(farm1.getId()), ids(foundForFarm1));
    }

    @Test
    void findByFarmAndBlock_shouldReturnNothing_whenFarmOrBlockDiffers() {
        // Arrange
        save(null, "A");
        save(1, "A");
        // Act & Assert
        assertTrue(growthCalendarRepository.findByFarmAndBlock(2, "A").isEmpty());
        assertTrue(growthCalendarRepository.findByFarmAndBlock(null, "B").isEmpty());
    }
}
