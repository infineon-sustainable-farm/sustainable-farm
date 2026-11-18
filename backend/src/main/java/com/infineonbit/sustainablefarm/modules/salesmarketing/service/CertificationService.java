package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CertificationRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CertificationResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Certification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CertificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CertificationService {

    private final CertificationRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public CertificationService(CertificationRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CertificationResponse> getAll() {
        return repository.findAll().stream().map(CertificationResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CertificationResponse getById(Long id) {
        return CertificationResponse.of(find(id));
    }

    public CertificationResponse create(CertificationRequest request) {
        return CertificationResponse.of(repository.save(mapper.apply(request, new Certification())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public CertificationResponse update(Long id, CertificationRequest request) {
        return CertificationResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private Certification find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found with id: " + id));
    }
}
