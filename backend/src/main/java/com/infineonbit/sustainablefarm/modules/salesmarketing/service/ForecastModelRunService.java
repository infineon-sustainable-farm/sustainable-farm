package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ForecastModelRunRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ForecastModelRunResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ForecastModelRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ForecastModelRunService {

    private final ForecastModelRunRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public ForecastModelRunService(ForecastModelRunRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ForecastModelRunResponse> getAll() {
        return repository.findAll().stream().map(ForecastModelRunResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ForecastModelRunResponse getById(Long id) {
        return ForecastModelRunResponse.of(find(id));
    }

    public ForecastModelRunResponse create(ForecastModelRunRequest request) {
        return ForecastModelRunResponse.of(repository.save(mapper.apply(request, new ForecastModelRun())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public ForecastModelRunResponse update(Long id, ForecastModelRunRequest request) {
        return ForecastModelRunResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private ForecastModelRun find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ForecastModelRun not found with id: " + id));
    }
}
