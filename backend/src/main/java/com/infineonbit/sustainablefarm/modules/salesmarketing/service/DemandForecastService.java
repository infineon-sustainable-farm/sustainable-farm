package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandForecastRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.DemandForecastResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandForecastRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DemandForecastService {

    private final DemandForecastRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public DemandForecastService(DemandForecastRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DemandForecastResponse> getAll() {
        return repository.findAll().stream().map(DemandForecastResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public DemandForecastResponse getById(Long id) {
        return DemandForecastResponse.of(find(id));
    }

    public DemandForecastResponse create(DemandForecastRequest request) {
        return DemandForecastResponse.of(repository.save(mapper.apply(request, new DemandForecast())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public DemandForecastResponse update(Long id, DemandForecastRequest request) {
        return DemandForecastResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private DemandForecast find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandForecast not found with id: " + id));
    }
}
