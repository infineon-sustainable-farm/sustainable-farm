package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TourStopRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.WorkshopRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EducationalProgramServiceImpl implements EducationalProgramService {

    private final TourStopRepository tourStopRepository;
    private final WorkshopRepository workshopRepository;
    private final PersonDirectory personDirectory;

    public EducationalProgramServiceImpl(TourStopRepository tourStopRepository,
                                         WorkshopRepository workshopRepository,
                                         PersonDirectory personDirectory) {
        this.tourStopRepository = tourStopRepository;
        this.workshopRepository = workshopRepository;
        this.personDirectory = personDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourStopResponse> listStops() {
        return tourStopRepository.findAllByOrderByPositionAsc().stream()
                .map(TourStopResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourStopResponse> listStops(int page, int size) {
        return tourStopRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "position")))
                .map(TourStopResponse::from);
    }

    @Override
    @Transactional
    public TourStopResponse createStop(TourStopRequest request) {
        assertPositionFree(request.getPosition(), null);
        TourStop stop = new TourStop();
        apply(stop, request);
        return TourStopResponse.from(tourStopRepository.save(stop));
    }

    @Override
    @Transactional
    public TourStopResponse updateStop(Long id, TourStopRequest request) {
        TourStop stop = getStopEntity(id);
        assertPositionFree(request.getPosition(), id);
        apply(stop, request);
        tourStopRepository.save(stop);
        tourStopRepository.flush();
        return TourStopResponse.from(stop);
    }

    @Override
    @Transactional
    public void deactivateStop(Long id) {
        TourStop stop = getStopEntity(id);
        stop.setActive(false);
        tourStopRepository.save(stop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkshopResponse> listWorkshops(WorkshopStatus status) {
        List<Workshop> workshops = status == null
                ? workshopRepository.findAll()
                : workshopRepository.findByStatus(status);
        Map<Long, String> facilitators = personDirectory.namesOf(
                workshops.stream().map(Workshop::getFacilitatorPersonId).toList());
        return workshops.stream()
                .map(w -> WorkshopResponse.from(w, facilitators.get(w.getFacilitatorPersonId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkshopResponse> listWorkshops(WorkshopStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Workshop> workshops = status != null
                ? workshopRepository.findByStatus(status, pageable)
                : workshopRepository.findAll(pageable);
        Map<Long, String> facilitators = personDirectory.namesOf(
                workshops.getContent().stream().map(Workshop::getFacilitatorPersonId).toList());
        return workshops.map(w -> WorkshopResponse.from(w, facilitators.get(w.getFacilitatorPersonId())));
    }

    @Override
    @Transactional
    public WorkshopResponse createWorkshop(WorkshopRequest request) {
        Workshop workshop = new Workshop();
        apply(workshop, request);
        workshop.setStatus(WorkshopStatus.DRAFT);
        return toResponse(workshopRepository.save(workshop));
    }

    @Override
    @Transactional
    public WorkshopResponse updateWorkshop(Long id, WorkshopRequest request) {
        Workshop workshop = getWorkshopEntity(id);
        apply(workshop, request);
        workshopRepository.save(workshop);
        workshopRepository.flush();
        return toResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse publishWorkshop(Long id) {
        Workshop workshop = getWorkshopEntity(id);
        if (workshop.getStatus() != WorkshopStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT workshops can be published (current: "
                    + workshop.getStatus() + ")");
        }
        workshop.setStatus(WorkshopStatus.ACTIVE);
        workshopRepository.save(workshop);
        workshopRepository.flush();
        return toResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse deactivateWorkshop(Long id) {
        Workshop workshop = getWorkshopEntity(id);
        if (workshop.getStatus() != WorkshopStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE workshops can be deactivated (current: "
                    + workshop.getStatus() + ")");
        }
        workshop.setStatus(WorkshopStatus.INACTIVE);
        workshopRepository.save(workshop);
        workshopRepository.flush();
        return toResponse(workshop);
    }

    private void assertPositionFree(int position, Long excludeId) {
        boolean taken = excludeId == null
                ? tourStopRepository.existsByActiveTrueAndPosition(position)
                : tourStopRepository.existsByActiveTrueAndPositionAndIdNot(position, excludeId);
        if (taken) {
            throw new ConflictException("Another active stop already uses position " + position);
        }
    }

    private void apply(TourStop stop, TourStopRequest request) {
        stop.setName(request.getName());
        stop.setPosition(request.getPosition());
        stop.setDescription(request.getDescription());
        stop.setDurationMinutes(request.getDurationMinutes());
        stop.setMaxCapacity(request.getMaxCapacity());
        stop.setLocation(request.getLocation());
        stop.setDemo(request.getDemo());
        stop.setSafetyNotes(request.getSafetyNotes());
    }

    private void apply(Workshop workshop, WorkshopRequest request) {
        workshop.setName(request.getName());
        workshop.setDurationMinutes(request.getDurationMinutes());
        workshop.setTargetGroup(request.getTargetGroup());
        workshop.setFacilitatorPersonId(personDirectory.idOf(request.getFacilitator()));
        workshop.setDescription(request.getDescription());
    }

    private WorkshopResponse toResponse(Workshop workshop) {
        return WorkshopResponse.from(workshop, personDirectory.nameOf(workshop.getFacilitatorPersonId()));
    }

    private TourStop getStopEntity(Long id) {
        return tourStopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour stop " + id + " not found"));
    }

    private Workshop getWorkshopEntity(Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop " + id + " not found"));
    }
}