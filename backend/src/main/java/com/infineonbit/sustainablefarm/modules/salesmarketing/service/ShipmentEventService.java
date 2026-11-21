package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentEventRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ShipmentEventResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ShipmentEventService {

    private final ShipmentEventRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public ShipmentEventService(ShipmentEventRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ShipmentEventResponse> getAll() {
        return repository.findAll().stream().map(ShipmentEventResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentEventResponse getById(Long id) {
        return ShipmentEventResponse.of(find(id));
    }

    public ShipmentEventResponse create(ShipmentEventRequest request) {
        return ShipmentEventResponse.of(repository.save(mapper.apply(request, new ShipmentEvent())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public ShipmentEventResponse update(Long id, ShipmentEventRequest request) {
        return ShipmentEventResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private ShipmentEvent find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentEvent not found with id: " + id));
    }
}
