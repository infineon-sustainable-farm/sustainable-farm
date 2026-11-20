package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignMetricRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CampaignMetricResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CampaignMetricService {

    private final CampaignMetricRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public CampaignMetricService(CampaignMetricRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CampaignMetricResponse> getAll() {
        return repository.findAll().stream().map(CampaignMetricResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CampaignMetricResponse getById(Long id) {
        return CampaignMetricResponse.of(find(id));
    }

    public CampaignMetricResponse create(CampaignMetricRequest request) {
        return CampaignMetricResponse.of(repository.save(mapper.apply(request, new CampaignMetric())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public CampaignMetricResponse update(Long id, CampaignMetricRequest request) {
        return CampaignMetricResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private CampaignMetric find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CampaignMetric not found with id: " + id));
    }
}
