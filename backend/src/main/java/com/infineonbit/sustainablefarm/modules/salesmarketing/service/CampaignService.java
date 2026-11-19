package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CampaignResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CampaignService {

    private final CampaignRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public CampaignService(CampaignRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getAll() {
        return repository.findAll().stream().map(CampaignResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getById(Long id) {
        return CampaignResponse.of(find(id));
    }

    public CampaignResponse create(CampaignRequest request) {
        return CampaignResponse.of(repository.save(mapper.apply(request, new Campaign())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public CampaignResponse update(Long id, CampaignRequest request) {
        return CampaignResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private Campaign find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
    }
}
