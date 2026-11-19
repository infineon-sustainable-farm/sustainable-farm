package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelTargetRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.SalesChannelTargetResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannelTarget;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesChannelTargetService {

    private final SalesChannelTargetRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public SalesChannelTargetService(SalesChannelTargetRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SalesChannelTargetResponse> getAll() {
        return repository.findAll().stream().map(SalesChannelTargetResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public SalesChannelTargetResponse getById(Long id) {
        return SalesChannelTargetResponse.of(find(id));
    }

    public SalesChannelTargetResponse create(SalesChannelTargetRequest request) {
        return SalesChannelTargetResponse.of(repository.save(mapper.apply(request, new SalesChannelTarget())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public SalesChannelTargetResponse update(Long id, SalesChannelTargetRequest request) {
        return SalesChannelTargetResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private SalesChannelTarget find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SalesChannelTarget not found with id: " + id));
    }
}
