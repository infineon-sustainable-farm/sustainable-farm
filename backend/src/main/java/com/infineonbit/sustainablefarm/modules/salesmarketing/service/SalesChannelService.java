package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.SalesChannelRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.SalesChannelResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.SalesChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesChannelService {

    private final SalesChannelRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public SalesChannelService(SalesChannelRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SalesChannelResponse> getAll() {
        return repository.findAll().stream().map(SalesChannelResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public SalesChannelResponse getById(Long id) {
        return SalesChannelResponse.of(find(id));
    }

    public SalesChannelResponse create(SalesChannelRequest request) {
        return SalesChannelResponse.of(repository.save(mapper.apply(request, new SalesChannel())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public SalesChannelResponse update(Long id, SalesChannelRequest request) {
        return SalesChannelResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * The exception of an unknown id is still a plain {@code RuntimeException} (500), as before.
     *
     * @throws RuntimeException if no row has this id
     */
    private SalesChannel find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SalesChannel not found with id: " + id));
    }
}
