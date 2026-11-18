package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ShipmentRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ShipmentResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public ShipmentService(ShipmentRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAll() {
        return repository.findAll().stream().map(ShipmentResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id) {
        return ShipmentResponse.of(find(id));
    }

    public ShipmentResponse create(ShipmentRequest request) {
        return ShipmentResponse.of(repository.save(mapper.apply(request, new Shipment())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public ShipmentResponse update(Long id, ShipmentRequest request) {
        return ShipmentResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private Shipment find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));
    }
}
