package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorStopRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorStopResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorStop;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorStopRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Visitor Management tour stops mapped to water zones and sources.
 *
 * <p>Visitor Management identifies its stops by a name and a position only, and keeps it that way:
 * the water team maps on its side the stops that pass by water facilities (for example "Smart drip
 * irrigation"). Stops that are not mapped are not water stops and are ignored.</p>
 */
@Service
@Transactional(readOnly = true)
public class WaterVisitorStopService {
    private final WaterVisitorStopRepository stopRepository;
    private final FieldZoneRepository zoneRepository;
    private final WaterSourceRepository sourceRepository;

    public WaterVisitorStopService(WaterVisitorStopRepository stopRepository, FieldZoneRepository zoneRepository,
                                   WaterSourceRepository sourceRepository) {
        this.stopRepository = stopRepository;
        this.zoneRepository = zoneRepository;
        this.sourceRepository = sourceRepository;
    }

    public List<VisitorStopResponse> findAll() {
        return stopRepository.findAllByOrderByStopNameAsc().stream().map(VisitorStopResponse::from).toList();
    }

    /** Mapping of a stop name sent by Visitor Management (case and surrounding spaces ignored). */
    public Optional<WaterVisitorStop> resolve(String stopName) {
        return stopName == null || stopName.isBlank()
                ? Optional.empty()
                : stopRepository.findByStopNameIgnoreCase(stopName.trim());
    }

    @Transactional
    public VisitorStopResponse create(VisitorStopRequest request) {
        return VisitorStopResponse.from(stopRepository.save(apply(new WaterVisitorStop(), request)));
    }

    @Transactional
    public VisitorStopResponse update(UUID id, VisitorStopRequest request) {
        return VisitorStopResponse.from(stopRepository.save(apply(getEntity(id), request)));
    }

    @Transactional
    public void delete(UUID id) {
        stopRepository.delete(getEntity(id));
    }

    private WaterVisitorStop apply(WaterVisitorStop stop, VisitorStopRequest request) {
        String name = request.stopName().trim();
        if (request.zoneId() == null && request.sourceId() == null) {
            throw new IllegalArgumentException("A visitor stop shows a zone, a source or both");
        }
        if (request.zoneId() != null && !zoneRepository.existsById(request.zoneId())) {
            throw new NotFoundException("Zone");
        }
        if (request.sourceId() != null && !sourceRepository.existsById(request.sourceId())) {
            throw new NotFoundException("WaterSource");
        }
        stopRepository.findByStopNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(stop.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "The stop \"" + name + "\" is already mapped");
                });
        stop.setStopName(name);
        stop.setZoneId(request.zoneId());
        stop.setSourceId(request.sourceId());
        return stop;
    }

    private WaterVisitorStop getEntity(UUID id) {
        return stopRepository.findById(id).orElseThrow(() -> new NotFoundException("Visitor stop"));
    }
}
