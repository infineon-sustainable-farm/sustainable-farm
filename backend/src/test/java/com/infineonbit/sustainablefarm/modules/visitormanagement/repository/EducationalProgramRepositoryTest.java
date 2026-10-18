package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EducationalProgramRepositoryTest {

    @Autowired
    private TourStopRepository tourStopRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    private TourStop createStop(String name, int position, boolean active) {
        TourStop s = new TourStop();
        s.setName(name);
        s.setPosition(position);
        s.setDurationMinutes(15);
        s.setActive(active);
        return tourStopRepository.save(s);
    }

    @Test
    void findAllByOrderByPositionAsc_returnsOrdered() {
        createStop("Wrap-up", 6, true);
        createStop("Welcome", 1, true);
        createStop("Orchard", 3, true);

        List<TourStop> result = tourStopRepository.findAllByOrderByPositionAsc();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TourStop::getPosition).containsExactly(1, 3, 6);
    }

    @Test
    void existsByActiveTrueAndPosition_trueForActiveStop() {
        createStop("Welcome", 1, true);

        assertThat(tourStopRepository.existsByActiveTrueAndPosition(1)).isTrue();
        assertThat(tourStopRepository.existsByActiveTrueAndPosition(2)).isFalse();
    }

    @Test
    void existsByActiveTrueAndPosition_ignoresInactiveStops() {
        createStop("Old stop", 1, false);

        assertThat(tourStopRepository.existsByActiveTrueAndPosition(1)).isFalse();
    }

    @Test
    void existsByActiveTrueAndPositionAndIdNot_excludesCurrentStop() {
        TourStop first = createStop("Welcome", 1, true);

        assertThat(tourStopRepository.existsByActiveTrueAndPositionAndIdNot(1, first.getId()))
                .isFalse();
        assertThat(tourStopRepository.existsByActiveTrueAndPositionAndIdNot(1, 999L))
                .isTrue();
    }

    @Test
    void workshop_findByStatus_filters() {
        Workshop active = new Workshop();
        active.setName("Standard farm tour");
        active.setDurationMinutes(100);
        active.setStatus(WorkshopStatus.ACTIVE);
        workshopRepository.save(active);

        Workshop draft = new Workshop();
        draft.setName("Mango tasting");
        draft.setDurationMinutes(40);
        draft.setStatus(WorkshopStatus.DRAFT);
        workshopRepository.save(draft);

        List<Workshop> result = workshopRepository.findByStatus(WorkshopStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Standard farm tour");
    }
}