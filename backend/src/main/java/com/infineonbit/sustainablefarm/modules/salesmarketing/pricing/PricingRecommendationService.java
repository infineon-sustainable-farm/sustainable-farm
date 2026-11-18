package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingRecommendationRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.PricingRecommendationResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingRecommendation;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.PricingRecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PricingRecommendationService {

    private final PricingRecommendationRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public PricingRecommendationService(PricingRecommendationRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PricingRecommendationResponse> getAll() {
        return repository.findAll().stream().map(PricingRecommendationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public PricingRecommendationResponse getById(Long id) {
        return PricingRecommendationResponse.of(find(id));
    }

    public PricingRecommendationResponse create(PricingRecommendationRequest request) {
        return PricingRecommendationResponse.of(repository.save(mapper.apply(request, new PricingRecommendation())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public PricingRecommendationResponse update(Long id, PricingRecommendationRequest request) {
        return PricingRecommendationResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private PricingRecommendation find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PricingRecommendation not found with id: " + id));
    }
}
