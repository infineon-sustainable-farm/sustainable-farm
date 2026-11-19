package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingHistoryRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.PricingHistoryResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PricingHistoryService {

    private final PricingHistoryRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public PricingHistoryService(PricingHistoryRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PricingHistoryResponse> getAll() {
        return repository.findAll().stream().map(PricingHistoryResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public PricingHistoryResponse getById(Long id) {
        return PricingHistoryResponse.of(find(id));
    }

    public PricingHistoryResponse create(PricingHistoryRequest request) {
        return PricingHistoryResponse.of(repository.save(mapper.apply(request, new PricingHistory())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public PricingHistoryResponse update(Long id, PricingHistoryRequest request) {
        return PricingHistoryResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private PricingHistory find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PricingHistory not found with id: " + id));
    }
}
