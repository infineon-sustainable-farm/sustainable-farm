package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CustomerContactRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CustomerContactResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerContact;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerContactService {

    private final CustomerContactRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public CustomerContactService(CustomerContactRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CustomerContactResponse> getAll() {
        return repository.findAll().stream().map(CustomerContactResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CustomerContactResponse getById(Long id) {
        return CustomerContactResponse.of(find(id));
    }

    public CustomerContactResponse create(CustomerContactRequest request) {
        return CustomerContactResponse.of(repository.save(mapper.apply(request, new CustomerContact())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public CustomerContactResponse update(Long id, CustomerContactRequest request) {
        return CustomerContactResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private CustomerContact find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerContact not found with id: " + id));
    }
}
