package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ReportFileRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ReportFileResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ReportFile;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ReportFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReportFileService {

    private final ReportFileRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public ReportFileService(ReportFileRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ReportFileResponse> getAll() {
        return repository.findAll().stream().map(ReportFileResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ReportFileResponse getById(Long id) {
        return ReportFileResponse.of(find(id));
    }

    public ReportFileResponse create(ReportFileRequest request) {
        return ReportFileResponse.of(repository.save(mapper.apply(request, new ReportFile())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public ReportFileResponse update(Long id, ReportFileRequest request) {
        return ReportFileResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private ReportFile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportFile not found with id: " + id));
    }
}
