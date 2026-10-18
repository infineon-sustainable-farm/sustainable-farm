package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TourStopRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.WorkshopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EducationalProgramServiceImplTest {

    @Mock
    private TourStopRepository tourStopRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @InjectMocks
    private EducationalProgramServiceImpl service;

    private Long stopId = 1L;
    private Long workshopId = 2L;

    private TourStop buildStop(int position) {
        TourStop stop = new TourStop();
        stop.setId(stopId);
        stop.setName("Mango orchard");
        stop.setPosition(position);
        stop.setDurationMinutes(20);
        stop.setMaxCapacity(15);
        stop.setActive(true);
        return stop;
    }

    private TourStopRequest buildStopRequest(int position) {
        TourStopRequest req = new TourStopRequest();
        req.setName("Mango orchard");
        req.setPosition(position);
        req.setDurationMinutes(20);
        req.setMaxCapacity(15);
        return req;
    }

    private Workshop buildWorkshop(WorkshopStatus status) {
        Workshop w = new Workshop();
        w.setId(workshopId);
        w.setName("Standard farm tour");
        w.setDurationMinutes(100);
        w.setTargetGroup("All");
        w.setStatus(status);
        return w;
    }

    private WorkshopRequest buildWorkshopRequest() {
        WorkshopRequest req = new WorkshopRequest();
        req.setName("Standard farm tour");
        req.setDurationMinutes(100);
        req.setTargetGroup("All");
        return req;
    }

    @Test
    void createStop_success() {
        when(tourStopRepository.existsByActiveTrueAndPosition(1)).thenReturn(false);
        when(tourStopRepository.save(any(TourStop.class))).thenAnswer(inv -> inv.getArgument(0));

        TourStopResponse response = service.createStop(buildStopRequest(1));

        assertThat(response.getName()).isEqualTo("Mango orchard");
        assertThat(response.getPosition()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void createStop_duplicatePosition_throws() {
        when(tourStopRepository.existsByActiveTrueAndPosition(1)).thenReturn(true);

        assertThatThrownBy(() -> service.createStop(buildStopRequest(1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("position");
    }

    @Test
    void updateStop_success() {
        when(tourStopRepository.findById(stopId)).thenReturn(Optional.of(buildStop(2)));
        when(tourStopRepository.existsByActiveTrueAndPositionAndIdNot(1, stopId)).thenReturn(false);
        when(tourStopRepository.save(any(TourStop.class))).thenAnswer(inv -> inv.getArgument(0));

        TourStopRequest req = buildStopRequest(1);
        req.setName("Updated name");
        TourStopResponse response = service.updateStop(stopId, req);

        assertThat(response.getName()).isEqualTo("Updated name");
        assertThat(response.getPosition()).isEqualTo(1);
    }

    @Test
    void updateStop_positionTakenByOtherStop_throws() {
        when(tourStopRepository.findById(stopId)).thenReturn(Optional.of(buildStop(6)));
        when(tourStopRepository.existsByActiveTrueAndPositionAndIdNot(1, stopId)).thenReturn(true);

        assertThatThrownBy(() -> service.updateStop(stopId, buildStopRequest(1)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivateStop_success() {
        TourStop stop = buildStop(1);
        when(tourStopRepository.findById(stopId)).thenReturn(Optional.of(stop));
        when(tourStopRepository.save(any(TourStop.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateStop(stopId);

        assertThat(stop.isActive()).isFalse();
    }

    @Test
    void listStops_ordered() {
        TourStop first = buildStop(1);
        TourStop second = buildStop(2);
        when(tourStopRepository.findAllByOrderByPositionAsc()).thenReturn(List.of(first, second));

        List<TourStopResponse> responses = service.listStops();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getPosition()).isEqualTo(1);
    }

    @Test
    void createWorkshop_defaultDraft() {
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopResponse response = service.createWorkshop(buildWorkshopRequest());

        assertThat(response.getName()).isEqualTo("Standard farm tour");
        assertThat(response.getStatus()).isEqualTo(WorkshopStatus.DRAFT);
    }

    @Test
    void publishWorkshop_success() {
        when(workshopRepository.findById(workshopId)).thenReturn(Optional.of(buildWorkshop(WorkshopStatus.DRAFT)));
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopResponse response = service.publishWorkshop(workshopId);

        assertThat(response.getStatus()).isEqualTo(WorkshopStatus.ACTIVE);
    }

    @Test
    void publishWorkshop_alreadyActive_throws() {
        when(workshopRepository.findById(workshopId)).thenReturn(Optional.of(buildWorkshop(WorkshopStatus.ACTIVE)));

        assertThatThrownBy(() -> service.publishWorkshop(workshopId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void deactivateWorkshop_success() {
        when(workshopRepository.findById(workshopId)).thenReturn(Optional.of(buildWorkshop(WorkshopStatus.ACTIVE)));
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopResponse response = service.deactivateWorkshop(workshopId);

        assertThat(response.getStatus()).isEqualTo(WorkshopStatus.INACTIVE);
    }

    @Test
    void deactivateWorkshop_notActive_throws() {
        when(workshopRepository.findById(workshopId)).thenReturn(Optional.of(buildWorkshop(WorkshopStatus.DRAFT)));

        assertThatThrownBy(() -> service.deactivateWorkshop(workshopId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void listWorkshops_byStatus() {
        Workshop active = buildWorkshop(WorkshopStatus.ACTIVE);
        when(workshopRepository.findByStatus(WorkshopStatus.ACTIVE)).thenReturn(List.of(active));

        List<WorkshopResponse> responses = service.listWorkshops(WorkshopStatus.ACTIVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(WorkshopStatus.ACTIVE);
    }
}