package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CompetitorPriceRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CompetitorPriceResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CompetitorPrice;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CompetitorPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompetitorPriceService {

    private final CompetitorPriceRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public CompetitorPriceService(CompetitorPriceRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CompetitorPriceResponse> getAll() {
        return repository.findAll().stream().map(CompetitorPriceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CompetitorPriceResponse getById(Long id) {
        return CompetitorPriceResponse.of(find(id));
    }

    public CompetitorPriceResponse create(CompetitorPriceRequest request) {
        return CompetitorPriceResponse.of(repository.save(mapper.apply(request, new CompetitorPrice())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public CompetitorPriceResponse update(Long id, CompetitorPriceRequest request) {
        return CompetitorPriceResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private CompetitorPrice find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CompetitorPrice not found with id: " + id));
    }
}
