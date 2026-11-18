package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandReportRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.DemandReportResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReport;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DemandReportService {

    private final DemandReportRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public DemandReportService(DemandReportRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DemandReportResponse> getAll() {
        return repository.findAll().stream().map(DemandReportResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public DemandReportResponse getById(Long id) {
        return DemandReportResponse.of(find(id));
    }

    public DemandReportResponse create(DemandReportRequest request) {
        return DemandReportResponse.of(repository.save(mapper.apply(request, new DemandReport())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public DemandReportResponse update(Long id, DemandReportRequest request) {
        return DemandReportResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private DemandReport find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandReport not found with id: " + id));
    }
}
